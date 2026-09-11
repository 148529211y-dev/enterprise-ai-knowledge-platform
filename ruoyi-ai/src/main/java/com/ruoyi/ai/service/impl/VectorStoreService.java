package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.KbChunk;
import com.ruoyi.ai.mapper.KbChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量存储服务 —— 内存向量检索 + MySQL 持久化
 *
 * 设计说明（面试要点）：
 *
 * Q: 为什么用向量数据库而不是 MySQL 直接查？
 * A: MySQL 擅长结构化查询（WHERE name = '张三'），但无法高效做"语义相似度搜索"。
 *    向量数据库（如 Qdrant/Milvus）专门优化了高维向量的近似最近邻(ANN)检索。
 *    本项目为演示目的使用内存向量 + 余弦相似度，生产环境应切换为 Qdrant。
 *
 * Q: 为什么选 Qdrant？
 * A: 1. Rust 实现，性能好   2. Spring AI 原生支持
 *    3. 单容器部署简单      4. 社区活跃
 *
 * 架构：
 *   MySQL → 持久化文档元数据和切片文本（保证数据不丢失）
 *   内存  → 缓存向量用于快速检索（启动时从 MySQL 加载）
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    /**
     * 内存向量索引：chunkId → 向量
     * 生产环境替换为 Qdrant/Milvus
     */
    private final ConcurrentHashMap<Long, float[]> vectorIndex = new ConcurrentHashMap<>();

    /**
     * 切片内容缓存：chunkId → 切片信息
     */
    private final ConcurrentHashMap<Long, ChunkEntry> chunkCache = new ConcurrentHashMap<>();

    private final KbChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;

    public VectorStoreService(KbChunkMapper chunkMapper, EmbeddingService embeddingService) {
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
    }

    /**
     * 启动时从数据库加载已有切片（简化版：实际应增量加载）
     */
    @PostConstruct
    public void init() {
        log.info("向量存储服务初始化...");
        // 启动时暂不加载，按需向量化
    }

    /**
     * 存储文档切片的向量
     *
     * @param chunks  从数据库读取的切片列表
     * @param vectors 对应的向量列表
     */
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

    /**
     * 向量相似度检索 —— 返回最相似的 TopK 个切片
     *
     * 面试知识点：TopK 选择
     *   K=3~5 适合大多数场景：
     *   - K 太小：可能遗漏关键信息
     *   - K 太大：引入噪声，消耗 token
     *   本项目默认 K=3
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (vectorIndex.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算所有向量与查询向量的余弦相似度
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Long, float[]> entry : vectorIndex.entrySet()) {
            double score = EmbeddingService.cosineSimilarity(queryVector, entry.getValue());
            ChunkEntry chunk = chunkCache.get(entry.getKey());
            if (chunk != null) {
                results.add(new SearchResult(chunk.docId, chunk.content, score));
            }
        }

        // 按相似度降序排序，取 TopK
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 删除某文档的所有向量
     */
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

    public int getVectorCount() {
        return vectorIndex.size();
    }

    // ==================== 数据结构 ====================

    private static class ChunkEntry {
        Long id;
        Long docId;
        Integer index;
        String content;

        ChunkEntry(Long id, Long docId, Integer index, String content) {
            this.id = id; this.docId = docId; this.index = index; this.content = content;
        }
    }

    /**
     * 检索结果
     */
    public static class SearchResult {
        public final Long docId;
        public final String content;
        public final double score;

        public SearchResult(Long docId, String content, double score) {
            this.docId = docId; this.content = content; this.score = score;
        }

        @Override
        public String toString() {
            return String.format("[doc=%d, score=%.4f, content=%s]", docId, score,
                    content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }
    }
}
