package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.dto.ChatResponse;

/**
 * Agent 智能助手服务接口
 */
public interface AgentService {

    /** Agent对话（含工具调用） */
    ChatResponse chat(Long userId, String sessionId, String userMessage);
}
