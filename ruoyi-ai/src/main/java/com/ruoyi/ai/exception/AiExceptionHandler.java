package com.ruoyi.ai.exception;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AI模块全局异常处理器
 *
 * 面试知识点：
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * 统一捕获 Controller 层异常，返回标准化 JSON 响应 {code, msg, data}
 *
 * 异常优先级：精确异常 > 通用异常 > 兜底 Exception
 */
@RestControllerAdvice(basePackages = "com.ruoyi.ai.controller")
public class AiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    /** 参数校验异常 — @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return AjaxResult.error(400, message);
    }

    /** 参数绑定异常 */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        return AjaxResult.error(400, message);
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return AjaxResult.error(405, "不支持" + e.getMethod() + "请求");
    }

    /** LLM调用异常 */
    @ExceptionHandler(LlmCallException.class)
    public AjaxResult handleLlmException(LlmCallException e, HttpServletRequest request) {
        log.error("LLM调用异常: uri={}, message={}", request.getRequestURI(), e.getMessage(), e);
        return AjaxResult.error(e.getCode(), "AI服务调用失败: " + e.getMessage());
    }

    /** Embedding异常 */
    @ExceptionHandler(EmbeddingException.class)
    public AjaxResult handleEmbeddingException(EmbeddingException e, HttpServletRequest request) {
        log.error("Embedding异常: uri={}", request.getRequestURI(), e);
        return AjaxResult.error(e.getCode(), "向量化服务异常: " + e.getMessage());
    }

    /** 工具执行异常 */
    @ExceptionHandler(ToolExecutionException.class)
    public AjaxResult handleToolException(ToolExecutionException e) {
        log.error("工具执行异常: {}", e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    /** 文档处理异常 */
    @ExceptionHandler(DocumentProcessException.class)
    public AjaxResult handleDocException(DocumentProcessException e) {
        log.error("文档处理异常: {}", e.getMessage(), e);
        return AjaxResult.error(e.getCode(), "文档处理失败: " + e.getMessage());
    }

    /** AI业务异常（通用） */
    @ExceptionHandler(AiException.class)
    public AjaxResult handleAiException(AiException e) {
        log.error("AI业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}", request.getRequestURI(), e);
        return AjaxResult.error(500, "系统内部错误，请联系管理员");
    }
}
