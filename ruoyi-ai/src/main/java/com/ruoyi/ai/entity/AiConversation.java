package com.ruoyi.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * AI对话历史实体
 */
public class AiConversation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String sessionId;
    /** 角色: user/assistant/system/tool */
    private String role;
    private String content;
    /** 消息类型: chat/rag/agent */
    private String msgType;
    private String toolName;
    private String toolArgs;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolArgs() { return toolArgs; }
    public void setToolArgs(String toolArgs) { this.toolArgs = toolArgs; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
