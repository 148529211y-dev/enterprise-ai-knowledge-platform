package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.entity.AiToolLog;
import com.ruoyi.ai.mapper.AiToolLogMapper;
import com.ruoyi.ai.service.ConversationCacheService;
import com.ruoyi.ai.service.LlmService;
import com.ruoyi.ai.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private LlmService llmService;
    @Mock
    private ToolRegistry toolRegistry;
    @Mock
    private ConversationCacheService conversationCacheService;
    @Mock
    private AiConfig aiConfig;
    @Mock
    private AiToolLogMapper toolLogMapper;

    @InjectMocks
    private AgentServiceImpl agentService;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "sess-001";

    @BeforeEach
    void setUp() {
        lenient().when(aiConfig.getMaxContextMessages()).thenReturn(20);
        lenient().when(conversationCacheService.getRecentMessages(anyLong(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());
        lenient().when(toolRegistry.getToolsSchema()).thenReturn(new JSONArray());
    }

    @Test
    void chat_noToolCalls_returnsDirectAnswer() {
        JSONObject llmResp = buildLlmResponse("Hello, how can I help?");
        when(llmService.chatWithTools(anyList(), any())).thenReturn(llmResp);

        ChatResponse resp = agentService.chat(USER_ID, SESSION_ID, "Hi");

        assertEquals("Hello, how can I help?", resp.getReply());
        assertEquals("agent", resp.getMode());
        assertTrue(resp.getToolCalls().isEmpty());
        verify(conversationCacheService, times(2)).saveMessage(any(AiConversation.class));
    }

    @Test
    void chat_singleToolCall_thenFinalAnswer() {
        JSONObject toolCallResp = buildToolCallResponse("call-1", "search", "{\"query\":\"test\"}");
        JSONObject finalResp = buildLlmResponse("Found results for test");
        when(llmService.chatWithTools(anyList(), any()))
                .thenReturn(toolCallResp)
                .thenReturn(finalResp);
        when(toolRegistry.executeTool("search", "{\"query\":\"test\"}")).thenReturn("search results");

        ChatResponse resp = agentService.chat(USER_ID, SESSION_ID, "search test");

        assertEquals("Found results for test", resp.getReply());
        assertEquals(1, resp.getToolCalls().size());
        assertEquals("search", resp.getToolCalls().get(0).getName());
        assertEquals("search results", resp.getToolCalls().get(0).getResult());
        verify(toolRegistry).executeTool("search", "{\"query\":\"test\"}");
    }

    @Test
    void chat_twoRoundToolCalling_works() {
        JSONObject toolCallResp1 = buildToolCallResponse("call-1", "lookup", "{\"id\":\"1\"}");
        JSONObject toolCallResp2 = buildToolCallResponse("call-2", "enrich", "{\"data\":\"x\"}");
        JSONObject finalResp = buildLlmResponse("Enriched result");
        when(llmService.chatWithTools(anyList(), any()))
                .thenReturn(toolCallResp1)
                .thenReturn(toolCallResp2)
                .thenReturn(finalResp);
        when(toolRegistry.executeTool(eq("lookup"), anyString())).thenReturn("lookup result");
        when(toolRegistry.executeTool(eq("enrich"), anyString())).thenReturn("enrich result");

        ChatResponse resp = agentService.chat(USER_ID, SESSION_ID, "complex query");

        assertEquals("Enriched result", resp.getReply());
        assertEquals(2, resp.getToolCalls().size());
        assertEquals("lookup", resp.getToolCalls().get(0).getName());
        assertEquals("enrich", resp.getToolCalls().get(1).getName());
    }

    @Test
    void chat_toolCall_writesLogToMapper() {
        JSONObject toolCallResp = buildToolCallResponse("call-1", "getData", "{\"key\":\"val\"}");
        JSONObject finalResp = buildLlmResponse("done");
        when(llmService.chatWithTools(anyList(), any()))
                .thenReturn(toolCallResp)
                .thenReturn(finalResp);
        when(toolRegistry.executeTool(eq("getData"), anyString())).thenReturn("data result");

        agentService.chat(USER_ID, SESSION_ID, "get data");

        ArgumentCaptor<AiToolLog> captor = ArgumentCaptor.forClass(AiToolLog.class);
        verify(toolLogMapper).insert(captor.capture());
        AiToolLog logEntry = captor.getValue();
        assertEquals(USER_ID, logEntry.getUserId());
        assertEquals(SESSION_ID, logEntry.getSessionId());
        assertEquals("getData", logEntry.getToolName());
        assertEquals("{\"key\":\"val\"}", logEntry.getInputArgs());
        assertEquals("data result", logEntry.getOutput());
        assertEquals(1, logEntry.getStatus());
    }

    @Test
    void chat_llmReturnsNull_returnsUnavailableMessage() {
        when(llmService.chatWithTools(anyList(), any())).thenReturn(null);

        ChatResponse resp = agentService.chat(USER_ID, SESSION_ID, "hello");

        assertTrue(resp.getReply().contains("不可用"));
    }

    @Test
    void chat_multipleToolCallsInOneRound_allExecuted() {
        JSONObject llmResp = buildToolCallResponse("call-1", "toolA", "{\"x\":1}");
        llmResp.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                .getJSONArray("tool_calls").add(buildToolCallObj("call-2", "toolB", "{\"y\":2}"));

        JSONObject finalResp = buildLlmResponse("all done");
        when(llmService.chatWithTools(anyList(), any()))
                .thenReturn(llmResp)
                .thenReturn(finalResp);
        when(toolRegistry.executeTool(eq("toolA"), anyString())).thenReturn("resultA");
        when(toolRegistry.executeTool(eq("toolB"), anyString())).thenReturn("resultB");

        ChatResponse resp = agentService.chat(USER_ID, SESSION_ID, "multi");

        assertEquals("all done", resp.getReply());
        verify(toolRegistry).executeTool("toolA", "{\"x\":1}");
        verify(toolRegistry).executeTool("toolB", "{\"y\":2}");
        verify(toolLogMapper, times(2)).insert(any(AiToolLog.class));
    }

    @Test
    void chat_savesUserAndAssistantMessages() {
        JSONObject llmResp = buildLlmResponse("reply");
        when(llmService.chatWithTools(anyList(), any())).thenReturn(llmResp);

        agentService.chat(USER_ID, SESSION_ID, "msg");

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationCacheService, times(2)).saveMessage(captor.capture());
        List<AiConversation> saved = captor.getAllValues();
        assertEquals("user", saved.get(0).getRole());
        assertEquals("msg", saved.get(0).getContent());
        assertEquals("assistant", saved.get(1).getRole());
        assertEquals("reply", saved.get(1).getContent());
    }

    private JSONObject buildLlmResponse(String content) {
        JSONObject message = new JSONObject();
        message.put("content", content);
        JSONObject choice = new JSONObject();
        choice.put("message", message);
        JSONArray choices = new JSONArray();
        choices.add(choice);
        JSONObject resp = new JSONObject();
        resp.put("choices", choices);
        return resp;
    }

    private JSONObject buildToolCallResponse(String callId, String funcName, String args) {
        JSONObject message = new JSONObject();
        message.put("content", "");
        message.put("tool_calls", Collections.singletonList(buildToolCallObj(callId, funcName, args)));
        JSONObject choice = new JSONObject();
        choice.put("message", message);
        JSONArray choices = new JSONArray();
        choices.add(choice);
        JSONObject resp = new JSONObject();
        resp.put("choices", choices);
        return resp;
    }

    private JSONObject buildToolCallObj(String callId, String funcName, String args) {
        JSONObject toolCall = new JSONObject();
        toolCall.put("id", callId);
        JSONObject function = new JSONObject();
        function.put("name", funcName);
        function.put("arguments", args);
        toolCall.put("function", function);
        return toolCall;
    }
}
