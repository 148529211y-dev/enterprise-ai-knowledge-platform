package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.service.EmbeddingService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Embedding 向量化服务
 *
 * 面试知识点：
 * 1. 什么是 Embedding？—— 将文本映射为高维向量，语义相近的文本向量距离更近
 * 2. 为什么需要向量化？—— 传统关键词匹配无法理解语义（"请假" vs "休假申请"）
 * 3. 为什么不用 MySQL 做向量检索？—— MySQL 不支持高效的高维向量相似度计算
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    private final AiConfig config;
    private final OkHttpClient httpClient;

    public EmbeddingServiceImpl(AiConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 将单条文本转为向量
     */
    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * 批量文本向量化（减少 API 调用次数）
     *
     * API: POST /v1/embeddings
     * Request:  { "model": "...", "input": ["text1", "text2"] }
     * Response: { "data": [{ "embedding": [0.1, ...] }] }
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        String url = resolveBaseUrl() + "/v1/embeddings";

        JSONObject body = new JSONObject();
        body.put("model", config.getEmbeddingModel());
        body.put("input", texts);

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")));

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer " + config.getApiKey());
        }

        try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                log.error("Embedding API 失败: code={}, body={}", response.code(),
                        response.body() != null ? response.body().string() : "null");
                return Collections.emptyList();
            }

            String respStr = response.body().string();
            JSONObject resp = JSON.parseObject(respStr);
            JSONArray data = resp.getJSONArray("data");

            List<float[]> vectors = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JSONArray arr = data.getJSONObject(i).getJSONArray("embedding");
                float[] vec = new float[arr.size()];
                for (int j = 0; j < arr.size(); j++) {
                    vec[j] = arr.getFloat(j);
                }
                vectors.add(vec);
            }
            return vectors;
        } catch (IOException e) {
            log.error("Embedding API 网络异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 余弦相似度计算 —— 向量检索的核心算法
     *
     * 面试知识点：
     * cosine(A, B) = (A·B) / (|A| × |B|)
     * 值域 [-1, 1]，越接近 1 表示语义越相似
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    private String resolveBaseUrl() {
        String base = config.getBaseUrl();
        if ("ollama".equalsIgnoreCase(config.getProvider())) {
            if (base == null || base.isEmpty() || base.contains("api.deepseek")) {
                return "http://localhost:11434";
            }
        }
        return base;
    }
}
