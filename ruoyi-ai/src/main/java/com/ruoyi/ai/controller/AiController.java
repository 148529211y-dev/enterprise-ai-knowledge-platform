package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.dto.ChatRequest;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.service.impl.AgentService;
import com.ruoyi.ai.service.impl.LlmService;
import com.ruoyi.ai.service.impl.RagService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * AI 智能问答接口
 *
 * 支持三种模式：
 *   1. chat  —— 普通对话（直接调 LLM）
 *   2. rag   —— 知识库问答（检索 + 生成）
 *   3. agent —— 智能助手（LLM + 工具调用）
 */
@RestController
@RequestMapping("/ai/chat")
public class AiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final LlmService llmService;
    private final RagService ragService;
    private final AgentService agentService;

    public AiController(LlmService llmService, RagService ragService, AgentService agentService) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentService = agentService;
    }

    /**
     * 统一聊天入口 —— 根据 mode 分发到不同处理链路
     */
    @PostMapping
    public AjaxResult chat(@RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getUserId();
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        String mode = request.getMode();

        if (message == null || message.trim().isEmpty()) {
            return AjaxResult.error("消息不能为空");
        }

        log.info("收到聊天请求: userId={}, mode={}, message={}", userId, mode, message);

        try {
            ChatResponse response;
            switch (mode) {
                case "rag":
                    response = ragService.ask(userId, sessionId, message);
                    break;
                case "agent":
                    response = agentService.chat(userId, sessionId, message);
                    break;
                default:
                    // 普通对话
                    String reply = llmService.chat(null, message);
                    response = new ChatResponse();
                    response.setReply(reply);
                    response.setMode("chat");
                    break;
            }
            return AjaxResult.success(response);
        } catch (Exception e) {
            log.error("聊天处理异常: mode={}", mode, e);
            return AjaxResult.error("处理失败: " + e.getMessage());
        }
    }
}
