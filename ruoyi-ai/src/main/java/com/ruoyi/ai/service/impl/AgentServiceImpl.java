package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.dto.ChatMessage;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.entity.AiToolLog;
import com.ruoyi.ai.mapper.AiToolLogMapper;
import com.ruoyi.ai.service.AgentService;
import com.ruoyi.ai.service.ConversationCacheService;
import com.ruoyi.ai.service.LlmService;
import com.ruoyi.ai.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    private static final String AGENT_SYSTEM_PROMPT = """
            你是一个企业智能办公助手。你可以帮助用户查询员工信息、部门信息，以及搜索企业知识库。
            规则：
            1. 根据用户的意图，判断是否需要调用工具
            2. 如果需要工具，使用提供的工具来获取准确信息，不要编造数据
            3. 如果不需要工具，直接回答用户问题
            4. 回答要简洁、专业、有帮助
            5. 如果工具返回了结果，基于结果给出清晰的总结
            """;

    private static final int MAX_TOOL_ROUNDS = 5;

    private final LlmService llmService;
    private final ToolRegistry toolRegistry;
    private final ConversationCacheService conversationCacheService;
    private final AiConfig aiConfig;
    private final AiToolLogMapper toolLogMapper;

    public AgentServiceImpl(LlmService llmService, ToolRegistry toolRegistry,
                            ConversationCacheService conversationCacheService,
                            AiConfig aiConfig, AiToolLogMapper toolLogMapper) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.conversationCacheService = conversationCacheService;
        this.aiConfig = aiConfig;
        this.toolLogMapper = toolLogMapper;
    }

    @Override
    public ChatResponse chat(Long userId, String sessionId, String userMessage) {
        log.info("Agent对话: userId={}, message={}", userId, userMessage);

        saveMessage(userId, sessionId, "user", userMessage, "agent");

        List<ChatMessage> messages = buildMessages(userId, sessionId);

        JSONArray tools = toolRegistry.getToolsSchema();

        List<ChatResponse.ToolCallInfo> toolCallLog = new ArrayList<>();
        String finalAnswer = null;

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JSONObject llmResp = llmService.chatWithTools(messages, tools);
            if (llmResp == null) {
                finalAnswer = "AI 服务暂时不可用，请稍后重试。";
                break;
            }

            JSONObject message = llmResp.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message");

            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                finalAnswer = message.getString("content");
                break;
            }

            String assistantContent = message.getString("content");
            messages.add(ChatMessage.assistant(
                    assistantContent != null ? assistantContent : "", toolCalls));

            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject call = toolCalls.getJSONObject(i);
                String callId = call.getString("id");
                JSONObject function = call.getJSONObject("function");
                String funcName = function.getString("name");
                String funcArgs = function.getString("arguments");

                log.info("执行工具: name={}, args={}", funcName, funcArgs);
                long startTime = System.currentTimeMillis();

                String result = toolRegistry.executeTool(funcName, funcArgs);
                long duration = System.currentTimeMillis() - startTime;

                log.info("工具结果: name={}, duration={}ms, result={}", funcName, duration,
                        result.length() > 100 ? result.substring(0, 100) + "..." : result);

                toolCallLog.add(new ChatResponse.ToolCallInfo(funcName, funcArgs, result));

                saveToolLog(userId, sessionId, funcName, funcArgs, result, 1, duration);

                messages.add(ChatMessage.toolResult(callId, result));
            }
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，处理过程中出现问题，请重试。";
        }

        saveMessage(userId, sessionId, "assistant", finalAnswer, "agent");

        ChatResponse resp = new ChatResponse();
        resp.setReply(finalAnswer);
        resp.setMode("agent");
        resp.setToolCalls(toolCallLog);
        return resp;
    }

    private List<ChatMessage> buildMessages(Long userId, String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(AGENT_SYSTEM_PROMPT));

        List<AiConversation> history = conversationCacheService.getRecentMessages(
                userId, sessionId, aiConfig.getMaxContextMessages());
        for (AiConversation msg : history) {
            messages.add(new ChatMessage(msg.getRole(),
                    msg.getContent() != null ? msg.getContent() : ""));
        }
        return messages;
    }

    private void saveMessage(Long userId, String sessionId, String role, String content, String msgType) {
        AiConversation msg = new AiConversation();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType);
        conversationCacheService.saveMessage(msg);
    }

    private void saveToolLog(Long userId, String sessionId, String toolName,
                             String args, String result, int status, long duration) {
        AiToolLog logEntry = new AiToolLog();
        logEntry.setUserId(userId);
        logEntry.setSessionId(sessionId);
        logEntry.setToolName(toolName);
        logEntry.setInputArgs(args);
        logEntry.setOutput(result);
        logEntry.setStatus(status);
        logEntry.setDuration(duration);
        toolLogMapper.insert(logEntry);
    }
}
