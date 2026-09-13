package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.KbChunk;
import com.ruoyi.ai.mapper.KbChunkMapper;
import com.ruoyi.ai.service.EmbeddingService;
import com.ruoyi.ai.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreServiceImpl.class);

    private final ConcurrentHashMap<Long, float[]> vectorIndex = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, ChunkEntry> chunkCache = new ConcurrentHashMap<>();

    private final KbChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;

    public VectorStoreServiceImpl(KbChunkMapper chunkMapper, EmbeddingService embeddingService) {
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void init() {
        log.info("向量存储服务初始化中...");
        List<KbChunk> allChunks = chunkMapper.selectAll();
        for (KbChunk chunk : allChunks) {
            chunkCache.put(chunk.getId(), new ChunkEntry(
                    chunk.getId(), chunk.getDocId(), chunk.getChunkIndex(), chunk.getContent()
            ));
        }
        log.info("启动加载完成，已缓存 {} 个切片", allChunks.size());
    }

    @Override
    public void store(List<KbChunk> chunks, List<float[]> vectors) {
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk chunk = chunks.get(i);
            vectorIndex.put(chunk.getId(), vectors.get(i));
            chunkCache.put(chunk.getId(), new ChunkEntry(
                    chunk.getId(), chunk.getDocId(), chunk.getChunkIndex(), chunk.getContent()
            ));
        }
        log.info("已存储 {} 个切片向量", chunks.size());
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (vectorIndex.isEmpty()) {
            return Collections.emptyList();
        }

        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, float[]> entry : vectorIndex.entrySet()) {
            double score = EmbeddingService.cosineSimilarity(queryVector, entry.getValue());
            ChunkEntry chunk = chunkCache.get(entry.getKey());
            if (chunk != null) {
                results.add(new SearchResult(chunk.docId, chunk.content, score));
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    @Override
    public void removeByDocId(Long docId) {
        List<Long> toRemove = new ArrayList<>();
        for (Map.Entry<Long, ChunkEntry> entry : chunkCache.entrySet()) {
            if (entry.getValue().docId.equals(docId)) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(id -> {
            vectorIndex.remove(id);
            chunkCache.remove(id);
        });
        log.info("已移除文档 {} 的 {} 个向量", docId, toRemove.size());
    }

    @Override
    public int getVectorCount() {
        return vectorIndex.size();
    }

    @Override
    public void rebuildIndex(List<KbChunk> chunks, List<float[]> vectors) {
        vectorIndex.clear();
        chunkCache.clear();
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk chunk = chunks.get(i);
            vectorIndex.put(chunk.getId(), vectors.get(i));
            chunkCache.put(chunk.getId(), new ChunkEntry(
                    chunk.getId(), chunk.getDocId(), chunk.getChunkIndex(), chunk.getContent()
            ));
        }
        log.info("索引重建完成，共 {} 个切片", chunks.size());
    }

    private static class ChunkEntry {
        Long id;
        Long docId;
        Integer index;
        String content;

        ChunkEntry(Long id, Long docId, Integer index, String content) {
            this.id = id; this.docId = docId; this.index = index; this.content = content;
        }
    }
}
