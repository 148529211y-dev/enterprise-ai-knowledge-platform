package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.AiConversation;
import com.ruoyi.ai.mapper.AiConversationMapper;
import com.ruoyi.ai.domain.dto.ChatResponse;
import com.ruoyi.ai.service.EmbeddingService;
import com.ruoyi.ai.service.RagService;
import com.ruoyi.ai.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 知识问答服务
 *
 * RAG = Retrieval Augmented Generation（检索增强生成）
 *
 * 完整流程（面试必讲）：
 *   用户提问
 *     ↓
 *   ① Query Embedding —— 将问题转为向量
 *     ↓
 *   ② 向量召回 —— 在向量库中找到最相似的 TopK 个文档切片
 *     ↓
 *   ③ Prompt 构造 —— 将检索到的文档片段作为上下文注入 Prompt
 *     ↓
 *   ④ LLM 生成 —— 基于上下文回答，而非自由发挥
 *     ↓
 *   ⑤ 返回答案 + 引用来源
 *
 * 面试知识点：
 *   Q: 如何解决 LLM 幻觉问题？
 *   A: 1. RAG 提供真实文档作为上下文，约束 LLM 基于事实回答
 *      2. Prompt 中明确要求「仅根据以下文档回答，如无相关信息请说明」
 *      3. 设置低 temperature（0.3）减少随机性
 *      4. 返回引用来源，用户可自行验证
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    /** RAG 系统提示词 */
    private static final String RAG_SYSTEM_PROMPT = """
            你是一个企业知识库问答助手。请严格根据以下提供的文档内容回答用户问题。
            规则：
            1. 仅根据提供的文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，请明确告知用户「根据现有知识库，未找到相关信息」
            3. 回答时请引用具体的文档片段作为依据
            4. 回答要简洁、准确、专业
            """;

    private final EmbeddingServiceImpl embeddingService;
    private final VectorStoreServiceImpl vectorStore;
    private final LlmServiceImpl llmService;
    private final AiConversationMapper conversationMapper;

    public RagServiceImpl(EmbeddingServiceImpl embeddingService, VectorStoreServiceImpl vectorStore,
                          LlmServiceImpl llmService, AiConversationMapper conversationMapper) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.llmService = llmService;
        this.conversationMapper = conversationMapper;
    }

    /**
     * RAG 问答主流程
     */
    @Override
    public ChatResponse ask(Long userId, String sessionId, String question) {
        log.info("RAG问答: userId={}, question={}", userId, question);

        // ① Query Embedding
        float[] queryVector = embeddingService.embed(question);
        if (queryVector.length == 0) {
            return buildResponse("Embedding 服务不可用，请检查 AI 配置。", null);
        }

        // ② 向量召回 —— TopK = 3
        List<VectorStoreService.SearchResult> results = vectorStore.search(queryVector, 3);
        if (results.isEmpty()) {
            saveConversation(userId, sessionId, "user", question, "rag");
            String reply = "知识库中暂无相关内容。您可以尝试上传相关文档后再提问。";
            saveConversation(userId, sessionId, "assistant", reply, "rag");
            return buildResponse(reply, null);
        }

        // ③ 构造带上下文的 Prompt
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

        // 保存用户消息
        saveConversation(userId, sessionId, "user", question, "rag");

        // ④ LLM 生成
        String answer = llmService.chat(RAG_SYSTEM_PROMPT, prompt);

        // 保存助手回复
        saveConversation(userId, sessionId, "assistant", answer, "rag");

        return buildResponse(answer, references);
    }

    private ChatResponse buildResponse(String reply, List<String> references) {
        ChatResponse resp = new ChatResponse();
        resp.setReply(reply);
        resp.setMode("rag");
        resp.setReferences(references);
        return resp;
    }

    private void saveConversation(Long userId, String sessionId, String role, String content, String msgType) {
        AiConversation msg = new AiConversation();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType);
        conversationMapper.insert(msg);
    }
}
