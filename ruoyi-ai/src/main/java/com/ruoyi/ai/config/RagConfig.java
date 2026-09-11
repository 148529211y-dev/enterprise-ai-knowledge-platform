package com.ruoyi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 模块配置
 *
 * 面试知识点：
 * 将 RAG 相关参数外部化配置，支持运行时调整，无需修改代码。
 * 例如：TopK 从 3 调到 5，只需改 application.yml。
 */
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class RagConfig {

    /** 向量召回数量（TopK） */
    private int topK = 3;

    /** 切片策略 */
    private ChunkStrategy chunkStrategy = ChunkStrategy.PARAGRAPH;

    /** LLM 生成温度（RAG 场景建议较低） */
    private double temperature = 0.3;

    /** 最大生成 Token 数 */
    private int maxTokens = 2048;

    /** 相似度阈值（低于此分数的结果不返回） */
    private double similarityThreshold = 0.3;

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public ChunkStrategy getChunkStrategy() { return chunkStrategy; }
    public void setChunkStrategy(ChunkStrategy chunkStrategy) { this.chunkStrategy = chunkStrategy; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
}
