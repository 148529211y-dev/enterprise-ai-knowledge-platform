package com.ruoyi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI模块配置 — application.yml 中 ai.* 前缀
 *
 * 支持两种模式：
 *   1. deepseek  — 调用 DeepSeek 云端 API（OpenAI 兼容格式）
 *   2. ollama    — 调用本地 Ollama 服务
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** 模式: deepseek / ollama */
    private String provider = "deepseek";

    /** API 地址 */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key */
    private String apiKey = "";

    /** 聊天模型名称 */
    private String chatModel = "deepseek-chat";

    /** Embedding 模型名称 */
    private String embeddingModel = "text-embedding-v3";

    /** Embedding 向量维度 */
    private int embeddingDimension = 1024;

    /** 最大上下文轮数（Memory窗口） */
    private int maxContextMessages = 20;

    // --- getter/setter ---

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public int getMaxContextMessages() { return maxContextMessages; }
    public void setMaxContextMessages(int maxContextMessages) { this.maxContextMessages = maxContextMessages; }
}
