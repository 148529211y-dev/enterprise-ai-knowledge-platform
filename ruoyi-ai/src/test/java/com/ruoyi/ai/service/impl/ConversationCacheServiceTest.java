package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.mapper.AiConversationMapper;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationCacheServiceTest {

    @Mock
    private RedisCache redisCache;
    @Mock
    private AiConversationMapper conversationMapper;

    @InjectMocks
    private ConversationCacheServiceImpl conversationCacheService;

    @Test
    void getRecentMessages_redisHit_returnsFromCache() {
        AiConversation msg = createMessage("user", "hello");
        when(redisCache.getCacheList("ai:chat:1:sess1"))
                .thenReturn(Collections.singletonList(msg));

        List<AiConversation> result = conversationCacheService.getRecentMessages(1L, "sess1", 10);

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).getContent());
        verifyNoInteractions(conversationMapper);
    }

    @Test
    void getRecentMessages_redisEmpty_fallsBackToMysql() {
        when(redisCache.getCacheList("ai:chat:1:sess1"))
                .thenReturn(Collections.emptyList());

        AiConversation dbMsg = createMessage("user", "from db");
        when(conversationMapper.selectRecent(1L, "sess1", 10))
                .thenReturn(Collections.singletonList(dbMsg));

        List<AiConversation> result = conversationCacheService.getRecentMessages(1L, "sess1", 10);

        assertEquals(1, result.size());
        assertEquals("from db", result.get(0).getContent());
        verify(conversationMapper).selectRecent(1L, "sess1", 10);
    }

    @Test
    void getRecentMessages_redisNull_fallsBackToMysql() {
        when(redisCache.getCacheList("ai:chat:1:sess1"))
                .thenReturn(null);

        AiConversation dbMsg = createMessage("user", "from db");
        when(conversationMapper.selectRecent(1L, "sess1", 10))
                .thenReturn(Collections.singletonList(dbMsg));

        List<AiConversation> result = conversationCacheService.getRecentMessages(1L, "sess1", 10);

        assertEquals(1, result.size());
        verify(conversationMapper).selectRecent(1L, "sess1", 10);
    }

    @Test
    void getRecentMessages_redisMiss_backfillsRedis() {
        when(redisCache.getCacheList("ai:chat:1:sess1"))
                .thenReturn(null);

        List<AiConversation> dbMessages = Arrays.asList(
                createMessage("user", "q1"),
                createMessage("assistant", "a1"));
        when(conversationMapper.selectRecent(1L, "sess1", 10))
                .thenReturn(dbMessages);

        conversationCacheService.getRecentMessages(1L, "sess1", 10);

        verify(redisCache).setCacheList("ai:chat:1:sess1", dbMessages);
        verify(redisCache).expire(eq("ai:chat:1:sess1"), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    void getRecentMessages_redisMissAndEmptyDb_doesNotBackfill() {
        when(redisCache.getCacheList("ai:chat:1:sess1"))
                .thenReturn(null);
        when(conversationMapper.selectRecent(1L, "sess1", 10))
                .thenReturn(Collections.emptyList());

        List<AiConversation> result = conversationCacheService.getRecentMessages(1L, "sess1", 10);

        assertTrue(result.isEmpty());
        verify(redisCache, never()).setCacheList(anyString(), anyList());
    }

    @Test
    void getRecentMessages_redisHitTruncatesToLimit() {
        List<AiConversation> many = Arrays.asList(
                createMessage("user", "m1"),
                createMessage("assistant", "m2"),
                createMessage("user", "m3"));
        doReturn(many).when(redisCache).getCacheList("ai:chat:1:sess1");

        List<AiConversation> result = conversationCacheService.getRecentMessages(1L, "sess1", 2);

        assertEquals(2, result.size());
        assertEquals("m2", result.get(0).getContent());
        assertEquals("m3", result.get(1).getContent());
    }

    @Test
    void saveMessage_writesToBothDbAndCache() {
        AiConversation msg = createMessage("user", "test");
        msg.setUserId(1L);
        msg.setSessionId("sess1");
        msg.setMsgType("chat");

        conversationCacheService.saveMessage(msg);

        verify(conversationMapper).insert(msg);
        verify(redisCache).setCacheList(eq("ai:chat:1:sess1"), anyList());
        verify(redisCache).expire(eq("ai:chat:1:sess1"), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    void evictSession_deletesRedisKey() {
        conversationCacheService.evictSession(1L, "sess1");

        verify(redisCache).deleteObject("ai:chat:1:sess1");
    }

    private AiConversation createMessage(String role, String content) {
        AiConversation msg = new AiConversation();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }
}
