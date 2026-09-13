package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.config.RagConfig;
import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final String RAG_SYSTEM_PROMPT = """
            你是一个企业知识库问答助手。请严格根据以下提供的文档内容回答用户问题。
            规则：
            1. 仅根据提供的文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，请明确告知用户「根据现有知识库，未找到相关信息」
            3. 回答时请引用具体的文档片段作为依据
            4. 回答要简洁、准确、专业
            """;

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStore;
    private final LlmService llmService;
    private final ConversationCacheService conversationCacheService;
    private final RagConfig ragConfig;

    public RagServiceImpl(EmbeddingService embeddingService, VectorStoreService vectorStore,
                          LlmService llmService, ConversationCacheService conversationCacheService,
                          RagConfig ragConfig) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.llmService = llmService;
        this.conversationCacheService = conversationCacheService;
        this.ragConfig = ragConfig;
    }

    @Override
    public ChatResponse ask(Long userId, String sessionId, String question) {
        log.info("RAG问答: userId={}, question={}", userId, question);

        float[] queryVector = embeddingService.embed(question);
        if (queryVector.length == 0) {
            return buildResponse("Embedding 服务不可用，请检查 AI 配置。", null);
        }

        List<VectorStoreService.SearchResult> rawResults = vectorStore.search(queryVector, ragConfig.getTopK());

        List<VectorStoreService.SearchResult> results = new ArrayList<>();
        for (VectorStoreService.SearchResult r : rawResults) {
            if (r.score >= ragConfig.getSimilarityThreshold()) {
                results.add(r);
            }
        }

        if (results.isEmpty()) {
            saveMessage(userId, sessionId, "user", question, "rag");
            String reply = "知识库中暂无相关内容。您可以尝试上传相关文档后再提问。";
            saveMessage(userId, sessionId, "assistant", reply, "rag");
            return buildResponse(reply, null);
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<String> references = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            VectorStoreService.SearchResult r = results.get(i);
            contextBuilder.append("【文档片段").append(i + 1).append("】\n");
            contextBuilder.append(r.content).append("\n\n");
            references.add(r.content);
        }

        String prompt = "以下是相关的知识库文档内容：\n\n" + contextBuilder +
                "用户问题：" + question + "\n\n请根据以上文档内容回答。";

        saveMessage(userId, sessionId, "user", question, "rag");

        String answer = llmService.chat(RAG_SYSTEM_PROMPT, prompt, ragConfig.getTemperature());

        saveMessage(userId, sessionId, "assistant", answer, "rag");

        return buildResponse(answer, references);
    }

    private ChatResponse buildResponse(String reply, List<String> references) {
        ChatResponse resp = new ChatResponse();
        resp.setReply(reply);
        resp.setMode("rag");
        resp.setReferences(references);
        return resp;
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
}
