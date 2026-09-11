package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.mapper.AiConversationMapper;
import com.ruoyi.ai.service.ConversationCacheService;
import com.ruoyi.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会话缓存服务实现 —— Redis + MySQL 双层存储
 *
 * Redis Key 设计：ai:chat:{userId}:{sessionId}
 * TTL：2小时（模拟真实场景中用户闲置后缓存自动清理）
 *
 * 面试知识点：
 * 为什么用 Redis 而不是只用 MySQL？
 * 1. 性能：Agent多轮对话需频繁读取历史，Redis微秒级 vs MySQL毫秒级
 * 2. TTL：Redis原生支持过期，MySQL需定时任务清理
 * 3. 数据结构：Redis List 天然适合存储时间序列消息
 */
@Service
public class ConversationCacheServiceImpl implements ConversationCacheService {

    private static final Logger log = LoggerFactory.getLogger(ConversationCacheServiceImpl.class);
    private static final String KEY_PREFIX = "ai:chat:";
    private static final long TTL_HOURS = 2;

    private final RedisCache redisCache;
    private final AiConversationMapper conversationMapper;

    public ConversationCacheServiceImpl(RedisCache redisCache, AiConversationMapper conversationMapper) {
        this.redisCache = redisCache;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public void saveMessage(AiConversation msg) {
        // 1. 写 MySQL（持久化）
        conversationMapper.insert(msg);

        // 2. 写 Redis（缓存）
        String redisKey = KEY_PREFIX + msg.getUserId() + ":" + msg.getSessionId();
        redisCache.setCacheList(redisKey, Collections.singletonList(msg));
        redisCache.expire(redisKey, TTL_HOURS, TimeUnit.HOURS);

        log.debug("消息已保存: userId={}, role={}, redisKey={}", msg.getUserId(), msg.getRole(), redisKey);
    }

    @Override
    public List<AiConversation> getRecentMessages(Long userId, String sessionId, int limit) {
        String redisKey = KEY_PREFIX + userId + ":" + sessionId;

        // 1. 尝试从 Redis 读取
        List<AiConversation> cached = redisCache.getCacheList(redisKey);
        if (cached != null && !cached.isEmpty()) {
            log.debug("Redis命中: userId={}, size={}", userId, cached.size());
            return tail(cached, limit);
        }

        // 2. Redis 未命中，从 MySQL 加载最近N条
        log.debug("Redis未命中，从MySQL加载: userId={}", userId);
        List<AiConversation> dbMessages = conversationMapper.selectRecent(userId, sessionId, limit);

        // 3. 回填 Redis
        if (!dbMessages.isEmpty()) {
            redisCache.setCacheList(redisKey, dbMessages);
            redisCache.expire(redisKey, TTL_HOURS, TimeUnit.HOURS);
        }

        return dbMessages;
    }

    @Override
    public void evictSession(Long userId, String sessionId) {
        String redisKey = KEY_PREFIX + userId + ":" + sessionId;
        redisCache.deleteObject(redisKey);
        log.info("已清除会话缓存: {}", redisKey);
    }

    /** 取列表最后N个元素 */
    private List<AiConversation> tail(List<AiConversation> list, int n) {
        if (list.size() <= n) return list;
        return new ArrayList<>(list.subList(list.size() - n, list.size()));
    }
}
