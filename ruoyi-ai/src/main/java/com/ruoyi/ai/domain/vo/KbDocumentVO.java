package com.ruoyi.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

/**
 * 知识库文档 VO —— 返回前端展示
 *
 * 与 Entity 的区别：
 * - Entity 对应数据库表，包含所有字段
 * - VO 只包含前端需要的字段，可做脱敏、格式化、额外计算
 */
public class KbDocumentVO {
    private Long id;
    private String title;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    /** 状态描述：待处理/已解析/已向量化/处理失败 */
    private String statusText;
    private Integer status;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    /** Entity → VO 转换 */
    public static KbDocumentVO fromEntity(com.ruoyi.ai.entity.KbDocument entity) {
        KbDocumentVO vo = new KbDocumentVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setFileType(entity.getFileType());
        vo.setFileSize(entity.getFileSize());
        vo.setChunkCount(entity.getChunkCount());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(switch (entity.getStatus()) {
            case 0 -> "待处理";
            case 1 -> "已解析";
            case 2 -> "已向量化";
            case 3 -> "处理失败";
            default -> "未知";
        });
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
