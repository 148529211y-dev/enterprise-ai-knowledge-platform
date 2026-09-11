package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LLM 调用服务 —— 直接通过 OkHttp 调用 OpenAI 兼容 API
 *
 * 设计说明（面试要点）：
 * 1. 为什么不用 Spring AI？ —— 直接调用更能体现对底层协议的理解，且无版本兼容问题
 * 2. 为什么用 OkHttp？ —— 支持连接池复用、超时控制、性能优于 RestTemplate
 * 3. 为什么同时支持 DeepSeek 和 Ollama？ —— 生产环境用云端API，演示时用本地模型
 *
 * API 格式: OpenAI Chat Completions (POST /v1/chat/completions)
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private final AiConfig config;
    private final OkHttpClient httpClient;

    public LlmService(AiConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build();
    }

    /**
     * 单轮对话（无历史）
     */
    public String chat(String systemPrompt, String userMessage) {
        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(makeMessage("system", systemPrompt));
        }
        messages.add(makeMessage("user", userMessage));
        return callChatApi(messages);
    }

    /**
     * 多轮对话（带历史上下文，用于 Agent / RAG）
     */
    public String chatWithHistory(List<Map<String, String>> messages) {
        JSONArray arr = new JSONArray();
        for (Map<String, String> msg : messages) {
            arr.add(makeMessage(msg.get("role"), msg.get("content")));
        }
        return callChatApi(arr);
    }

    /**
     * 带工具定义的对话 —— Function Calling
     *
     * @param messages   对话历史
     * @param tools      工具定义列表（OpenAI function calling 格式）
     * @return 完整的 LLM 响应 JSON（可能包含 tool_calls）
     */
    public JSONObject chatWithTools(List<Map<String, String>> messages, JSONArray tools) {
        JSONObject body = buildRequestBody(messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        return callChatApiRaw(body);
    }

    // ==================== 内部方法 ====================

    private String callChatApi(JSONArray messages) {
        JSONObject body = buildRequestBody(null);
        body.put("messages", messages);
        JSONObject resp = callChatApiRaw(body);
        if (resp == null) return "抱歉，AI服务暂时不可用，请稍后重试。";
        try {
            return resp.getJSONArray("choices")
                       .getJSONObject(0)
                       .getJSONObject("message")
                       .getString("content");
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", resp, e);
            return "抱歉，AI响应解析失败。";
        }
    }

    /**
     * 核心调用方法 —— 统一处理请求/响应
     *
     * 面试点：这里体现了对 HTTP 通信、JSON 解析、错误处理的理解
     */
    private JSONObject callChatApiRaw(JSONObject body) {
        String url = resolveBaseUrl() + "/v1/chat/completions";
        String jsonBody = body.toJSONString();
        log.debug("LLM请求 URL={}, Body={}", url, jsonBody);

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        // DeepSeek 需要 Authorization header，Ollama 不需要
        if (!config.getApiKey().isEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer " + config.getApiKey());
        }

        try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                log.error("LLM API 调用失败: code={}, body={}", response.code(),
                        response.body() != null ? response.body().string() : "null");
                return null;
            }
            String respStr = response.body().string();
            log.debug("LLM响应: {}", respStr);
            return JSON.parseObject(respStr);
        } catch (IOException e) {
            log.error("LLM API 网络异常", e);
            return null;
        }
    }

    private JSONObject buildRequestBody(List<Map<String, String>> messages) {
        JSONObject body = new JSONObject();
        body.put("model", config.getChatModel());
        body.put("temperature", 0.7);
        body.put("max_tokens", 2048);
        if (messages != null) {
            JSONArray arr = new JSONArray();
            for (Map<String, String> msg : messages) {
                arr.add(makeMessage(msg.get("role"), msg.get("content")));
            }
            body.put("messages", arr);
        }
        return body;
    }

    private JSONObject makeMessage(String role, String content) {
        JSONObject msg = new JSONObject();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String resolveBaseUrl() {
        String base = config.getBaseUrl();
        // Ollama 默认走本地 11434 端口
        if ("ollama".equalsIgnoreCase(config.getProvider())) {
            if (base == null || base.isEmpty() || base.contains("api.deepseek")) {
                return "http://localhost:11434";
            }
        }
        return base;
    }
}
