package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.dto.ChatMessage;
import com.ruoyi.ai.exception.LlmCallException;
import com.ruoyi.ai.service.LlmService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);
    private final AiConfig config;
    private final OkHttpClient httpClient;

    public LlmServiceImpl(AiConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    @Override
    public String chat(String systemPrompt, String userMessage, Double temperature) {
        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatMessage.system(systemPrompt).toJsonObject());
        }
        messages.add(ChatMessage.user(userMessage).toJsonObject());
        JSONObject body = buildRequestBody(temperature);
        body.put("messages", messages);
        JSONObject resp = callChatApi(body);
        try {
            return resp.getJSONArray("choices")
                       .getJSONObject(0)
                       .getJSONObject("message")
                       .getString("content");
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", resp, e);
            throw new LlmCallException("AI响应解析失败", e);
        }
    }

    @Override
    public String chatWithHistory(List<ChatMessage> messages) {
        JSONObject body = buildRequestBody(null);
        JSONArray arr = new JSONArray();
        for (ChatMessage msg : messages) {
            arr.add(msg.toJsonObject());
        }
        body.put("messages", arr);
        JSONObject resp = callChatApi(body);
        try {
            return resp.getJSONArray("choices")
                       .getJSONObject(0)
                       .getJSONObject("message")
                       .getString("content");
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", resp, e);
            throw new LlmCallException("AI响应解析失败", e);
        }
    }

    @Override
    public JSONObject chatWithTools(List<ChatMessage> messages, JSONArray tools) {
        JSONObject body = buildRequestBody(null);
        JSONArray arr = new JSONArray();
        for (ChatMessage msg : messages) {
            arr.add(msg.toJsonObject());
        }
        body.put("messages", arr);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        return callChatApi(body);
    }

    private JSONObject callChatApi(JSONObject body) {
        String url = resolveBaseUrl() + "/v1/chat/completions";
        String jsonBody = body.toJSONString();
        log.debug("LLM请求 URL={}, Body={}", url, jsonBody);

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        if (!config.getApiKey().isEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer " + config.getApiKey());
        }

        try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                String respBody = response.body() != null ? response.body().string() : "null";
                log.error("LLM API 调用失败: code={}, body={}", response.code(), respBody);
                throw new LlmCallException("LLM API调用失败: code=" + response.code());
            }
            String respStr = response.body().string();
            log.debug("LLM响应: {}", respStr);
            return JSON.parseObject(respStr);
        } catch (IOException e) {
            log.error("LLM API 网络异常", e);
            throw new LlmCallException("LLM API网络异常", e);
        }
    }

    private JSONObject buildRequestBody(Double temperature) {
        JSONObject body = new JSONObject();
        body.put("model", config.getChatModel());
        body.put("temperature", temperature != null ? temperature : 0.7);
        body.put("max_tokens", 2048);
        return body;
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
