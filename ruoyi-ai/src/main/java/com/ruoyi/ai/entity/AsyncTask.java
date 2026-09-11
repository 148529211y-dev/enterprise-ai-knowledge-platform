package com.ruoyi.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * 异步任务实体 —— 跟踪文档处理等耗时任务的状态
 *
 * 面试知识点：
 * 用户上传文档后，解析+切片+Embedding可能需要数秒到数十秒。
 * 采用异步处理：上传后立即返回 taskId，前端轮询查询状态。
 * 任务状态机：WAITING → RUNNING → SUCCESS / FAILED
 */
public class AsyncTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskId;
    /** 任务类型: doc_process(文档处理) / doc_reindex(重新索引) */
    private String taskType;
    /** 关联的业务ID（如文档ID） */
    private Long bizId;
    /** 任务状态: WAITING / RUNNING / SUCCESS / FAILED */
    private String status;
    /** 进度描述 */
    private String progress;
    /** 错误信息 */
    private String errorMessage;
    private Long createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
