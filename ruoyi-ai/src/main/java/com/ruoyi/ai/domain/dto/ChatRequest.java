package com.ruoyi.ai.domain.dto;

/**
 * 聊天请求 DTO
 */
public class ChatRequest {
    /** 会话ID，前端生成，同一轮对话保持不变 */
    private String sessionId;
    /** 用户消息 */
    private String message;
    /** 聊天模式: chat(普通) / rag(知识问答) / agent(智能助手) */
    private String mode = "chat";

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
