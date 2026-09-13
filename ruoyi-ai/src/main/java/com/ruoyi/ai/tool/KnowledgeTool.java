package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.service.VectorStoreService;
import com.ruoyi.ai.service.EmbeddingService;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "search_knowledge", description = "从企业知识库中搜索相关文档，当用户询问公司制度、流程、规范等问题时使用")
@Component
public class KnowledgeTool implements AiTool {

    private final VectorStoreService vectorStore;
    private final EmbeddingService embeddingService;

    public KnowledgeTool(VectorStoreService vectorStore, EmbeddingService embeddingService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
    }

    @Override
    public String execute(String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        String query = args.getString("query");
        try {
            float[] queryVector = embeddingService.embed(query);
            if (queryVector.length == 0) {
                return "Embedding 服务不可用，无法搜索知识库。";
            }
            List<VectorStoreService.SearchResult> results = vectorStore.search(queryVector, 3);
            if (results.isEmpty()) {
                return "知识库中未找到与「" + query + "」相关的内容。";
            }
            StringBuilder sb = new StringBuilder("搜索到以下相关内容：\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append(i + 1).append(". ").append(results.get(i).content).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "知识库搜索失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject queryProp = new JSONObject();
        queryProp.put("type", "string");
        queryProp.put("description", "搜索关键词或问题");

        JSONObject properties = new JSONObject();
        properties.put("query", queryProp);

        schema.put("properties", properties);
        schema.put("required", List.of("query"));
        return schema;
    }
}
