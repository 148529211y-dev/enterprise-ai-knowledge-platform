package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.KbChunk;
import java.util.List;

/**
 * 向量存储服务接口
 *
 * 面向接口编程：当前实现为内存向量，未来可无缝切换 Qdrant/Milvus
 */
public interface VectorStoreService {

    /** 存储切片向量 */
    void store(List<KbChunk> chunks, List<float[]> vectors);

    /** 向量相似度检索 */
    List<SearchResult> search(float[] queryVector, int topK);

    /** 删除某文档的全部向量 */
    void removeByDocId(Long docId);

    /** 当前向量总数 */
    int getVectorCount();

    /** 检索结果 */
    class SearchResult {
        public final Long docId;
        public final String content;
        public final double score;
        public SearchResult(Long docId, String content, double score) {
            this.docId = docId; this.content = content; this.score = score;
        }
    }
}
