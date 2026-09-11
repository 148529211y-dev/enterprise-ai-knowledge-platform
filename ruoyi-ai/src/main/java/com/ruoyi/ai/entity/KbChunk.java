package com.ruoyi.ai.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 文档切片实体
 */
public class KbChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long docId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
