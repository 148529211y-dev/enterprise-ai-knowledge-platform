package com.ruoyi.ai.exception;

/**
 * AI模块业务异常基类
 *
 * 设计说明：
 * 继承 RuntimeException（非受检异常），配合 @RestControllerAdvice 统一拦截。
 * 错误码体系：100xx = AI模块，细分 10001=LLM异常、10002=Embedding异常等。
 */
public class AiException extends RuntimeException {

    private final int code;

    public AiException(String message) {
        super(message);
        this.code = 10000;
    }

    public AiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
