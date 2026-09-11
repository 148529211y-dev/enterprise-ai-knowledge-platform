package com.ruoyi.ai.exception;

/** 文档处理异常（解析失败、切片失败等） */
public class DocumentProcessException extends AiException {
    public DocumentProcessException(String message) { super(10004, message); }
    public DocumentProcessException(String message, Throwable cause) { super(10004, message, cause); }
}
