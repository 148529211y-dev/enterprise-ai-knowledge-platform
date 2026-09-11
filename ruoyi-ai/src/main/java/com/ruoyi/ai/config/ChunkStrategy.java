package com.ruoyi.ai.config;

/**
 * 文本切片策略枚举
 *
 * 面试知识点：
 * 不同文档类型适合不同的切片策略：
 * - PARAGRAPH：按段落切分，适合结构化文档（制度、手册）
 * - FIXED_SIZE：固定大小切分，适合无明显段落的长文本
 * - SENTENCE：按句子切分，适合对话记录、新闻
 */
public enum ChunkStrategy {

    /** 按段落切分（默认，保持语义完整性） */
    PARAGRAPH("paragraph", "按段落切分", 500, 50),

    /** 固定大小切分 */
    FIXED_SIZE("fixed_size", "固定大小切分", 500, 50),

    /** 按句子切分 */
    SENTENCE("sentence", "按句子切分", 300, 30);

    private final String code;
    private final String description;
    private final int maxChunkSize;
    private final int overlapSize;

    ChunkStrategy(String code, String description, int maxChunkSize, int overlapSize) {
        this.code = code;
        this.description = description;
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public int getMaxChunkSize() { return maxChunkSize; }
    public int getOverlapSize() { return overlapSize; }
}
