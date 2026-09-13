package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.exception.EmbeddingException;
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

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

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
                String respBody = response.body() != null ? response.body().string() : "null";
                log.error("Embedding API 失败: code={}, body={}", response.code(), respBody);
                throw new EmbeddingException("Embedding API调用失败: code=" + response.code());
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
            throw new EmbeddingException("Embedding API网络异常", e);
        }
    }

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
