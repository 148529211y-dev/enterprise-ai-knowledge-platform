package com.ruoyi.ai.entity;

import java.io.Serializable;
import java.util.Date;

public class AiEvaluation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String question;
    private String expectedAnswer;
    private String actualAnswer;
    private String mode;
    private Integer responseTime;
    private Integer toolCount;
    private Double retrievalScore;
    private Integer evalScore;
    private String evalResult;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public String getActualAnswer() { return actualAnswer; }
    public void setActualAnswer(String actualAnswer) { this.actualAnswer = actualAnswer; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getResponseTime() { return responseTime; }
    public void setResponseTime(Integer responseTime) { this.responseTime = responseTime; }
    public Integer getToolCount() { return toolCount; }
    public void setToolCount(Integer toolCount) { this.toolCount = toolCount; }
    public Double getRetrievalScore() { return retrievalScore; }
    public void setRetrievalScore(Double retrievalScore) { this.retrievalScore = retrievalScore; }
    public Integer getEvalScore() { return evalScore; }
    public void setEvalScore(Integer evalScore) { this.evalScore = evalScore; }
    public String getEvalResult() { return evalResult; }
    public void setEvalResult(String evalResult) { this.evalResult = evalResult; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
