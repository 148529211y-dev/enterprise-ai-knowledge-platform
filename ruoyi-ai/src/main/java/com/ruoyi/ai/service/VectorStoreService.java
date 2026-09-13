package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.KbChunk;
import java.util.List;

public interface VectorStoreService {

    void store(List<KbChunk> chunks, List<float[]> vectors);

    List<SearchResult> search(float[] queryVector, int topK);

    void removeByDocId(Long docId);

    int getVectorCount();

    void rebuildIndex(List<KbChunk> chunks, List<float[]> vectors);

    class SearchResult {
        public final Long docId;
        public final String content;
        public final double score;
        public SearchResult(Long docId, String content, double score) {
            this.docId = docId; this.content = content; this.score = score;
        }
    }
}
