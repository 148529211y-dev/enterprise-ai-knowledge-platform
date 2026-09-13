package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.KbChunk;
import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.mapper.KbChunkMapper;
import com.ruoyi.ai.mapper.KbDocumentMapper;
import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.ai.service.AsyncTaskService;
import com.ruoyi.ai.service.EmbeddingService;
import com.ruoyi.ai.service.KbService;
import com.ruoyi.ai.service.VectorStoreService;
import com.ruoyi.common.core.domain.AjaxResult;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class KbServiceImpl implements KbService {

    private static final Logger log = LoggerFactory.getLogger(KbServiceImpl.class);

    private static final int MAX_CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 50;

    private final KbDocumentMapper docMapper;
    private final KbChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStore;
    private final AsyncTaskService asyncTaskService;
    private final Tika tika = new Tika();

    private final String uploadDir;

    public KbServiceImpl(KbDocumentMapper docMapper, KbChunkMapper chunkMapper,
                         EmbeddingService embeddingService, VectorStoreService vectorStore,
                         AsyncTaskService asyncTaskService) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.asyncTaskService = asyncTaskService;
        this.uploadDir = System.getProperty("user.home") + "/ruoyi-upload/kb/";
    }

    @Override
    public List<KbDocument> listDocuments(KbDocumentQuery query) {
        KbDocument entity = new KbDocument();
        if (query != null) {
            entity.setTitle(query.getTitle());
            entity.setStatus(query.getStatus());
            entity.setCreateBy(query.getCreateBy());
        }
        return docMapper.selectList(entity);
    }

    @Override
    public KbDocument getById(Long id) {
        return docMapper.selectById(id);
    }

    @Override
    public AjaxResult uploadAndProcess(MultipartFile file, String title, String createBy) throws Exception {
        String originalName = file.getOriginalFilename();
        String fileType = getFileExtension(originalName);
        String savedName = UUID.randomUUID() + "." + fileType;
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        Path filePath = dirPath.resolve(savedName);
        file.transferTo(filePath.toFile());

        KbDocument doc = new KbDocument();
        doc.setTitle(title != null && !title.isEmpty() ? title : originalName);
        doc.setFilePath(filePath.toString());
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setCreateBy(createBy);
        doc.setStatus(0);
        saveDocumentRecord(doc);

        String taskId = asyncTaskService.submitDocProcessTask(doc.getId(), null,
                docId -> processDocumentAsync(docId));

        Map<String, Object> result = new HashMap<>();
        result.put("docId", doc.getId());
        result.put("taskId", taskId);
        return AjaxResult.success("文档上传成功", result);
    }

    @Override
    public void processDocument(KbDocument doc) throws Exception {
        processDocumentAsync(doc.getId());
    }

    @Override
    @Transactional
    public void deleteDocument(Long docId) {
        vectorStore.removeByDocId(docId);
        chunkMapper.deleteByDocId(docId);
        docMapper.deleteById(docId);
    }

    @Override
    public String reindexAll(Long userId) {
        KbDocument query = new KbDocument();
        query.setStatus(2);
        List<KbDocument> docs = docMapper.selectList(query);
        List<Long> docIds = new ArrayList<>();
        for (KbDocument doc : docs) {
            docIds.add(doc.getId());
        }
        return asyncTaskService.submitDocProcessTask(0L, userId, docId -> {
            for (Long id : docIds) {
                try {
                    updateDocStatus(id, 0);
                    processDocumentAsync(id);
                } catch (Exception e) {
                    log.error("reindex failed for docId={}", id, e);
                    updateDocStatus(id, 3);
                }
            }
        });
    }

    public void processDocumentAsync(Long docId) throws Exception {
        KbDocument doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new FileNotFoundException("文档不存在: " + docId);
        }

        String content = parseDocument(doc.getFilePath(), doc.getFileType());
        doc.setContent(content);
        doc.setStatus(1);
        docMapper.update(doc);
        log.info("文档解析完成: docId={}, 文本长度={}", doc.getId(), content.length());

        List<String> textChunks = chunkText(content);
        List<KbChunk> chunks = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            KbChunk chunk = new KbChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(textChunks.get(i));
            chunk.setTokenCount(estimateTokens(textChunks.get(i)));
            chunks.add(chunk);
        }
        saveChunks(doc.getId(), chunks);
        doc.setChunkCount(chunks.size());
        docMapper.update(doc);
        log.info("文本切片完成: docId={}, 切片数={}", doc.getId(), chunks.size());

        List<KbChunk> savedChunks = chunkMapper.selectByDocId(doc.getId());
        List<String> texts = new ArrayList<>();
        for (KbChunk c : savedChunks) {
            texts.add(c.getContent());
        }

        List<float[]> allVectors = new ArrayList<>();
        int batchSize = 16;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<float[]> batch = embeddingService.embedBatch(texts.subList(i, end));
            allVectors.addAll(batch);
            if (i + batchSize < texts.size()) {
                Thread.sleep(200);
            }
        }

        vectorStore.store(savedChunks, allVectors);
        updateDocStatus(doc.getId(), 2);
        log.info("文档向量化完成: docId={}, 向量数={}", doc.getId(), allVectors.size());
    }

    @Transactional
    public void saveDocumentRecord(KbDocument doc) {
        docMapper.insert(doc);
    }

    @Transactional
    public void saveChunks(Long docId, List<KbChunk> chunks) {
        if (!chunks.isEmpty()) {
            chunkMapper.insertBatch(chunks);
        }
    }

    @Transactional
    public void updateDocStatus(Long docId, int status) {
        KbDocument doc = new KbDocument();
        doc.setId(docId);
        doc.setStatus(status);
        docMapper.update(doc);
    }

    private String parseDocument(String filePath, String fileType) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        String rawText;
        if ("md".equals(fileType) || "txt".equals(fileType)) {
            rawText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } else {
            rawText = tika.parseToString(file);
        }
        return rawText.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");

        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() + 2 <= MAX_CHUNK_SIZE) {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            } else {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                }
                if (para.length() > MAX_CHUNK_SIZE) {
                    List<String> subChunks = splitLongText(para);
                    chunks.addAll(subChunks);
                    current = new StringBuilder();
                } else {
                    String prev = current.toString();
                    if (prev.length() > OVERLAP_SIZE) {
                        current = new StringBuilder(prev.substring(prev.length() - OVERLAP_SIZE));
                    } else {
                        current = new StringBuilder(prev);
                    }
                    current.append("\n\n").append(para);
                }
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private List<String> splitLongText(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                if (lastPeriod > start) end = lastPeriod + 1;
            }
            result.add(text.substring(start, end));
            start = end - OVERLAP_SIZE;
            if (start < 0) start = 0;
            if (start >= end) break;
        }
        return result;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "txt";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "txt";
    }

    private int estimateTokens(String text) {
        int chinese = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) chinese++;
            else other++;
        }
        return (int) (chinese / 1.5 + other / 4.0);
    }
}
