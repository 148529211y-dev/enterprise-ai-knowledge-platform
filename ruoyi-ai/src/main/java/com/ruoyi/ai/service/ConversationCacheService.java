package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.AiConversation;
import java.util.List;

/**
 * 会话缓存服务接口 —— Redis短期缓存 + MySQL长期存储
 *
 * 面试知识点：
 * 双层存储策略：
 * - Redis：短期上下文（最近20轮），读写微秒级，支持TTL自动过期
 * - MySQL：全量历史，永久保存，用于审计和回溯
 *
 * 读取优先级：Redis → MySQL（Redis未命中时从MySQL加载并回填）
 */
public interface ConversationCacheService {

    /** 保存消息（双写 Redis + MySQL） */
    void saveMessage(AiConversation msg);

    /** 获取最近N轮对话（优先Redis，降级MySQL） */
    List<AiConversation> getRecentMessages(Long userId, String sessionId, int limit);

    /** 清空某会话的Redis缓存 */
    void evictSession(Long userId, String sessionId);
}
