package com.ruoyi.ai.exception;

/** Embedding向量化异常 */
public class EmbeddingException extends AiException {
    public EmbeddingException(String message) { super(10002, message); }
    public EmbeddingException(String message, Throwable cause) { super(10002, message, cause); }
}
