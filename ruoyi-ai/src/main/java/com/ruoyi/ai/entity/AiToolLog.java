package com.ruoyi.ai.entity;

import java.io.Serializable;
import java.util.Date;

public class AiToolLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String sessionId;
    private String toolName;
    private String inputArgs;
    private String output;
    private Integer status;
    private Long duration;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getInputArgs() { return inputArgs; }
    public void setInputArgs(String inputArgs) { this.inputArgs = inputArgs; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
