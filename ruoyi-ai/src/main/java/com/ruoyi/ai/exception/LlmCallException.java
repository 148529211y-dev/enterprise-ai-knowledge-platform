package com.ruoyi.ai.exception;

/** LLM调用异常（网络超时、API返回错误等） */
public class LlmCallException extends AiException {
    public LlmCallException(String message) { super(10001, message); }
    public LlmCallException(String message, Throwable cause) { super(10001, message, cause); }
}
