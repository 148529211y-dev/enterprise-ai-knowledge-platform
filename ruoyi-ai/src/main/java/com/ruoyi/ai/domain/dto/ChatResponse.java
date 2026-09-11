package com.ruoyi.ai.domain.dto;

import java.util.List;

/**
 * 聊天响应 DTO
 */
public class ChatResponse {
    /** AI 回复内容 */
    private String reply;
    /** 消息类型: chat/rag/agent */
    private String mode;
    /** RAG 模式下引用的文档片段 */
    private List<String> references;
    /** Agent 模式下调用的工具列表 */
    private List<ToolCallInfo> toolCalls;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<String> getReferences() { return references; }
    public void setReferences(List<String> references) { this.references = references; }
    public List<ToolCallInfo> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallInfo> toolCalls) { this.toolCalls = toolCalls; }

    public static class ToolCallInfo {
        private String name;
        private String arguments;
        private String result;
        public ToolCallInfo() {}
        public ToolCallInfo(String name, String arguments, String result) {
            this.name = name; this.arguments = arguments; this.result = result;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
