package com.ruoyi.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * AI调用日志实体 —— 记录每次AI相关操作
 *
 * 面试知识点：
 * 央国企/银行系统对审计要求严格，所有AI调用必须留痕。
 * 记录内容：谁、问了什么、调了什么工具、花了多久、是否成功。
 */
public class AiAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String sessionId;
    /** 操作类型: chat/rag/agent/embed/tool */
    private String operationType;
    /** 用户问题 */
    private String question;
    /** 调用的工具名称（Agent模式） */
    private String toolName;
    /** 工具输入参数 */
    private String toolArgs;
    /** LLM回答（截取前500字） */
    private String answer;
    /** 消耗的Token数 */
    private Integer tokenCount;
    /** 响应时间(ms) */
    private Long responseTime;
    /** 状态: 1-成功 0-失败 */
    private Integer status;
    /** 错误信息 */
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolArgs() { return toolArgs; }
    public void setToolArgs(String toolArgs) { this.toolArgs = toolArgs; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
