package com.ruoyi.ai.entity;

import java.io.Serializable;
import java.util.Date;

public class AiBadCase implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String question;
    private String actualAnswer;
    private String failureReason;
    private String mode;
    private String optimization;
    private String status;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getActualAnswer() { return actualAnswer; }
    public void setActualAnswer(String actualAnswer) { this.actualAnswer = actualAnswer; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getOptimization() { return optimization; }
    public void setOptimization(String optimization) { this.optimization = optimization; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
