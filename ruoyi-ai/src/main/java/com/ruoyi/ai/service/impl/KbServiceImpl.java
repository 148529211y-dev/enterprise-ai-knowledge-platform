package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.KbChunk;
import com.ruoyi.ai.entity.KbDocument;
import com.ruoyi.ai.mapper.KbChunkMapper;
import com.ruoyi.ai.mapper.KbDocumentMapper;
import com.ruoyi.ai.domain.query.KbDocumentQuery;
import com.ruoyi.ai.service.KbService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库文档管理服务
 *
 * 职责：文档上传 → 文本解析 → 文本切片 → 调用 Embedding → 存入向量索引
 *
 * 面试知识 —— Chunk 切片策略：
 *   1. 按段落切分：以 \n\n 为界，保持语义完整性（本项目采用）
 *   2. 固定 Token 数切分：每 N 个 token 切一刀，简单但可能截断语义
 *   3. 滑动窗口：相邻切片有重叠，保证上下文连贯
 *   本项目采用「按段落 + 最大长度限制 + 重叠」的混合策略
 */
@Service
public class KbServiceImpl implements KbService {

    private static final Logger log = LoggerFactory.getLogger(KbServiceImpl.class);

    /** 单个切片最大字符数 */
    private static final int MAX_CHUNK_SIZE = 500;
    /** 相邻切片重叠字符数 */
    private static final int OVERLAP_SIZE = 50;

    private final KbDocumentMapper docMapper;
    private final KbChunkMapper chunkMapper;
    private final EmbeddingServiceImpl embeddingService;
    private final VectorStoreServiceImpl vectorStore;
    private final Tika tika = new Tika();

    /** 文件上传根目录 */
    private final String uploadDir;

    public KbServiceImpl(KbDocumentMapper docMapper, KbChunkMapper chunkMapper,
                         EmbeddingServiceImpl embeddingService, VectorStoreServiceImpl vectorStore) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.uploadDir = System.getProperty("user.home") + "/ruoyi-upload/kb/";
    }

    /**
     * 文档列表查询
     */
    @Override
    public List<KbDocument> listDocuments(KbDocumentQuery query) {
        // Query → Entity 转换（Mapper 层保持接收 Entity）
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

    /**
     * 上传并处理文档 —— 完整链路
     *
     * 流程：上传 → 解析 → 切片 → Embedding → 存储
     */
    @Override
    @Transactional
    public AjaxResult uploadAndProcess(MultipartFile file, String title, String createBy) throws Exception {
        // 1. 文件存储
        String originalName = file.getOriginalFilename();
        String fileType = getFileExtension(originalName);
        String savedName = UUID.randomUUID() + "." + fileType;
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        Path filePath = dirPath.resolve(savedName);
        file.transferTo(filePath.toFile());

        // 2. 保存文档记录
        KbDocument doc = new KbDocument();
        doc.setTitle(title != null && !title.isEmpty() ? title : originalName);
        doc.setFilePath(filePath.toString());
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setCreateBy(createBy);
        doc.setStatus(0); // 待处理
        docMapper.insert(doc);

        // 3. 处理文档（解析 → 切片 → 向量化）
        try {
            processDocument(doc);
        } catch (Exception e) {
            log.error("文档处理失败: docId={}", doc.getId(), e);
            doc.setStatus(3); // 失败
            docMapper.update(doc);
            throw e;
        }

        return AjaxResult.success("文档上传成功", doc);
    }

    /**
     * 文档处理核心流程
     */
    @Override
    @Transactional
    public void processDocument(KbDocument doc) throws Exception {
        // 1. 文本解析（Apache Tika）
        String content = parseDocument(doc.getFilePath(), doc.getFileType());
        doc.setContent(content);
        doc.setStatus(1); // 已解析
        docMapper.update(doc);
        log.info("文档解析完成: docId={}, 文本长度={}", doc.getId(), content.length());

        // 2. 文本切片
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
        if (!chunks.isEmpty()) {
            chunkMapper.insertBatch(chunks);
        }
        doc.setChunkCount(chunks.size());
        docMapper.update(doc);
        log.info("文本切片完成: docId={}, 切片数={}", doc.getId(), chunks.size());

        // 3. 向量化（Embedding）
        List<KbChunk> savedChunks = chunkMapper.selectByDocId(doc.getId());
        List<String> texts = new ArrayList<>();
        for (KbChunk c : savedChunks) {
            texts.add(c.getContent());
        }

        // 分批调用 Embedding API（每批最多 16 条）
        List<float[]> allVectors = new ArrayList<>();
        int batchSize = 16;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<float[]> batch = embeddingService.embedBatch(texts.subList(i, end));
            allVectors.addAll(batch);
            if (i + batchSize < texts.size()) {
                Thread.sleep(200); // 避免 API 限流
            }
        }

        // 4. 存入向量索引
        vectorStore.store(savedChunks, allVectors);
        doc.setStatus(2); // 已向量化
        docMapper.update(doc);
        log.info("文档向量化完成: docId={}, 向量数={}", doc.getId(), allVectors.size());
    }

    /**
     * 删除文档及其切片和向量
     */
    @Override
    @Transactional
    public void deleteDocument(Long docId) {
        vectorStore.removeByDocId(docId);
        chunkMapper.deleteByDocId(docId);
        docMapper.deleteById(docId);
    }

    // ==================== 内部方法 ====================

    /**
     * 文本解析 —— 使用 Apache Tika
     *
     * Tika 支持 PDF/Word/HTML/Markdown 等多种格式，自动识别文件类型
     */
    private String parseDocument(String filePath, String fileType) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        String rawText;
        if ("md".equals(fileType) || "txt".equals(fileType)) {
            // Markdown/纯文本直接读取
            rawText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } else {
            // PDF/Word 等使用 Tika 解析
            rawText = tika.parseToString(file);
        }
        // 清洗：去除多余空白行
        return rawText.replaceAll("\\n{3,}", "\n\n").trim();
    }

    /**
     * 文本切片 —— 按段落 + 最大长度 + 重叠
     *
     * 策略说明（面试要点）：
     * 1. 优先按段落（\n\n）切分，保持语义完整性
     * 2. 超长段落按 MAX_CHUNK_SIZE 再切，避免单片过大
     * 3. 相邻切片保留 OVERLAP_SIZE 字符重叠，保证上下文连贯
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        // 先按段落拆分
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
                // 超长段落需要再切
                if (para.length() > MAX_CHUNK_SIZE) {
                    List<String> subChunks = splitLongText(para);
                    chunks.addAll(subChunks);
                    current = new StringBuilder();
                } else {
                    // 保留重叠部分
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

    /** 超长文本按句号换行再切 */
    private List<String> splitLongText(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                // 尝试在句号处断开
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

    /** 粗略估算 token 数（中文约 1.5 字/token，英文约 4 字符/token） */
    private int estimateTokens(String text) {
        int chinese = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) chinese++;
            else other++;
        }
        return (int) (chinese / 1.5 + other / 4.0);
    }
}
