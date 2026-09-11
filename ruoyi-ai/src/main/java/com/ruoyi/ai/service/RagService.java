package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.dto.ChatResponse;

/**
 * RAG 知识问答服务接口
 */
public interface RagService {

    /** RAG问答 */
    ChatResponse ask(Long userId, String sessionId, String question);
}
