package com.ruoyi.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.mapper.AiConversationMapper;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.tool.ToolRegistry;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent 智能助手服务
 *
 * 面试知识点 —— Agent 架构：
 *   Agent = LLM + Memory + Tools
 *
 *   1. LLM（大脑）：理解用户意图，推理需要调用哪些工具，整合结果生成回答
 *   2. Memory（记忆）：保存对话历史，支持多轮上下文
 *   3. Tools（工具）：扩展 Agent 能力，如查询数据库、搜索知识库、调用 API
 *
 *   为什么用 Agent 而不是普通接口？
 *   - 普通接口：用户必须知道调用哪个接口、传什么参数
 *   - Agent：用户说自然语言，Agent 自动判断需要什么工具、怎么调用
 *   - Agent 能组合多个工具完成复杂任务（如「帮我查张三的部门负责人」需要调用两个工具）
 *
 * Function Calling 流程：
 *   用户消息 + 工具定义 → LLM
 *   LLM 返回: "我需要调用 query_employee(name='张三')"
 *   我们执行这个工具，把结果喂回 LLM
 *   LLM 整合结果，生成最终回答
 */
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

    private final LlmServiceImpl llmService;
    private final ToolRegistry toolRegistry;
    private final AiConversationMapper conversationMapper;
    private final AiConfig aiConfig;

    /** 最大工具调用轮次，防止无限循环 */
    private static final int MAX_TOOL_ROUNDS = 5;

    public AgentServiceImpl(LlmServiceImpl llmService, ToolRegistry toolRegistry,
                            AiConversationMapper conversationMapper, AiConfig aiConfig) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.conversationMapper = conversationMapper;
        this.aiConfig = aiConfig;
    }

    /**
     * Agent 对话主流程
     */
    @Override
    public ChatResponse chat(Long userId, String sessionId, String userMessage) {
        log.info("Agent对话: userId={}, message={}", userId, userMessage);

        // 1. 保存用户消息
        saveMessage(userId, sessionId, "user", userMessage, "agent");

        // 2. 构建对话历史（含 Memory）
        List<Map<String, String>> messages = buildMessages(userId, sessionId);

        // 3. 解析工具定义
        JSONArray tools = JSON.parseArray(toolRegistry.getToolsJson());

        // 4. Function Calling 循环
        List<ChatResponse.ToolCallInfo> toolCallLog = new ArrayList<>();
        String finalAnswer = null;

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            // 调用 LLM（带工具定义）
            JSONObject llmResp = llmService.chatWithTools(messages, tools);
            if (llmResp == null) {
                finalAnswer = "AI 服务暂时不可用，请稍后重试。";
                break;
            }

            JSONObject message = llmResp.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message");

            // 检查 LLM 是否要求调用工具
            JSONArray toolCalls = message.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                // LLM 不需要调用工具，直接返回回答
                finalAnswer = message.getString("content");
                break;
            }

            // 5. LLM 要求调用工具 —— 执行工具
            // 先把 assistant 的 tool_calls 消息加入历史
            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", message.getString("content") != null ? message.getString("content") : "");
            assistantMsg.put("tool_calls", toolCalls);
            messages.add(mapFromJson(assistantMsg));

            // 逐个执行工具
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

                // 记录工具调用
                toolCallLog.add(new ChatResponse.ToolCallInfo(funcName, funcArgs, result));

                // 保存工具调用日志
                saveToolLog(userId, sessionId, funcName, funcArgs, result, duration);

                // 把工具结果加入对话历史
                JSONObject toolMsg = new JSONObject();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", callId);
                toolMsg.put("content", result);
                messages.add(mapFromJson(toolMsg));
            }
            // 继续循环，让 LLM 整合工具结果
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，处理过程中出现问题，请重试。";
        }

        // 6. 保存助手回复
        saveMessage(userId, sessionId, "assistant", finalAnswer, "agent");

        // 7. 构建响应
        ChatResponse resp = new ChatResponse();
        resp.setReply(finalAnswer);
        resp.setMode("agent");
        resp.setToolCalls(toolCallLog);
        return resp;
    }

    // ==================== 内部方法 ====================

    /**
     * 构建对话历史（从 DB 加载最近 N 轮）
     */
    private List<Map<String, String>> buildMessages(Long userId, String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt
        messages.add(Map.of("role", "system", "content", AGENT_SYSTEM_PROMPT));

        // 从数据库加载历史对话
        List<AiConversation> history = conversationMapper.selectRecent(
                userId, sessionId, aiConfig.getMaxContextMessages());
        for (AiConversation msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent() != null ? msg.getContent() : "");
            messages.add(m);
        }
        return messages;
    }

    private Map<String, String> mapFromJson(JSONObject json) {
        Map<String, String> map = new HashMap<>();
        map.put("role", json.getString("role"));
        map.put("content", json.getString("content") != null ? json.getString("content") : "");
        return map;
    }

    private void saveMessage(Long userId, String sessionId, String role, String content, String msgType) {
        AiConversation msg = new AiConversation();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType);
        conversationMapper.insert(msg);
    }

    private void saveToolLog(Long userId, String sessionId, String toolName,
                             String args, String result, long duration) {
        // 工具调用日志记录到 ai_tool_log 表
        // 简化实现：通过 AiConversation 表记录
        AiConversation msg = new AiConversation();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole("tool");
        msg.setContent(result);
        msg.setMsgType("agent");
        msg.setToolName(toolName);
        msg.setToolArgs(args);
        conversationMapper.insert(msg);
    }
}
