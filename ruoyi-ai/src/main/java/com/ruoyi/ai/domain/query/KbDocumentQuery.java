package com.ruoyi.ai.domain.query;

/**
 * 知识库文档查询条件对象
 *
 * 面试知识点：
 * Query 对象封装复杂查询条件，避免 Controller 参数列表过长。
 * 也便于后续接入 MyBatis Plus 的 QueryWrapper。
 */
public class KbDocumentQuery {
    private String title;
    private Integer status;
    private String fileType;
    private String createBy;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
}
