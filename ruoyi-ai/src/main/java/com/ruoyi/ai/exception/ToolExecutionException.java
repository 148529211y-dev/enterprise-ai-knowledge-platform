package com.ruoyi.ai.exception;

/** Agent工具执行异常 */
public class ToolExecutionException extends AiException {
    public ToolExecutionException(String message) { super(10003, message); }
    public ToolExecutionException(String toolName, String message) {
        super(10003, "工具[" + toolName + "]执行失败: " + message);
    }
    public ToolExecutionException(String toolName, Throwable cause) {
        super(10003, "工具[" + toolName + "]执行异常", cause);
    }
}
