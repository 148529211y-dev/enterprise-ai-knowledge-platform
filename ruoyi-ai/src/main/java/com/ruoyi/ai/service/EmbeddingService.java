package com.ruoyi.ai.service;

import java.util.List;

/**
 * Embedding 向量化服务接口
 */
public interface EmbeddingService {

    /** 单条文本向量化 */
    float[] embed(String text);

    /** 批量文本向量化 */
    List<float[]> embedBatch(List<String> texts);

    /** 余弦相似度计算 */
    static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i];
        }
        double d = Math.sqrt(na) * Math.sqrt(nb);
        return d == 0 ? 0.0 : dot / d;
    }
}
