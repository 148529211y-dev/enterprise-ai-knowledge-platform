package com.ruoyi.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求 DTO
 *
 * 面试知识点：
 * JSR 380 (Bean Validation 3.0) 注解配合 @Valid 触发校验，
 * 校验失败时抛出 MethodArgumentNotValidException，由全局异常处理器拦截。
 */
public class ChatRequest {

    /** 会话ID，前端生成，同一轮对话保持不变 */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 用户消息 */
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字")
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
