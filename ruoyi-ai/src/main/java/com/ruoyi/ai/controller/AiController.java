package com.ruoyi.ai.controller;

import com.ruoyi.ai.domain.dto.ChatRequest;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.entity.AiAuditLog;
import com.ruoyi.ai.mapper.AiAuditLogMapper;
import com.ruoyi.ai.service.AgentService;
import com.ruoyi.ai.service.LlmService;
import com.ruoyi.ai.service.RagService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/chat")
public class AiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final LlmService llmService;
    private final RagService ragService;
    private final AgentService agentService;
    private final AiAuditLogMapper auditLogMapper;

    public AiController(LlmService llmService, RagService ragService,
                        AgentService agentService, AiAuditLogMapper auditLogMapper) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentService = agentService;
        this.auditLogMapper = auditLogMapper;
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:use')")
    @PostMapping
    public AjaxResult chat(@Valid @RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getUserId();
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        String mode = request.getMode();

        log.info("收到聊天请求: userId={}, mode={}, message={}", userId, mode, message);

        long startTime = System.currentTimeMillis();
        AiAuditLog auditLog = new AiAuditLog();
        auditLog.setUserId(userId);
        auditLog.setSessionId(sessionId);
        auditLog.setOperationType(mode);
        auditLog.setQuestion(message);

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
                    String reply = llmService.chat(null, message);
                    response = new ChatResponse();
                    response.setReply(reply);
                    response.setMode("chat");
                    break;
            }

            long duration = System.currentTimeMillis() - startTime;
            auditLog.setAnswer(response.getReply() != null && response.getReply().length() > 500
                    ? response.getReply().substring(0, 500) : response.getReply());
            auditLog.setResponseTime(duration);
            auditLog.setStatus(1);
            auditLogMapper.insert(auditLog);

            return AjaxResult.success(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            auditLog.setResponseTime(duration);
            auditLog.setStatus(0);
            auditLog.setErrorMessage(e.getMessage());
            auditLogMapper.insert(auditLog);

            log.error("聊天处理异常: mode={}", mode, e);
            throw e;
        }
    }
}
