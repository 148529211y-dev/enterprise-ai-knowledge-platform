package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.exception.ToolExecutionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final List<AiTool> tools;
    private final Map<String, AiTool> toolMap = new LinkedHashMap<>();
    private final JSONArray toolsSchema = new JSONArray();

    public ToolRegistry(List<AiTool> tools) {
        this.tools = tools;
    }

    @PostConstruct
    public void init() {
        for (AiTool tool : tools) {
            AgentTool annotation = tool.getClass().getAnnotation(AgentTool.class);
            if (annotation == null) {
                continue;
            }
            String name = annotation.name();
            String description = annotation.description();

            toolMap.put(name, tool);

            JSONObject functionDef = new JSONObject();
            functionDef.put("name", name);
            functionDef.put("description", description);
            functionDef.put("parameters", tool.getParametersSchema());

            JSONObject toolDef = new JSONObject();
            toolDef.put("type", "function");
            toolDef.put("function", functionDef);

            toolsSchema.add(toolDef);
            log.info("Registered tool: {}", name);
        }
    }

    public JSONArray getToolsSchema() {
        return toolsSchema;
    }

    public String executeTool(String name, String argsJson) {
        AiTool tool = toolMap.get(name);
        if (tool == null) {
            throw new ToolExecutionException(name, "not found");
        }
        return tool.execute(argsJson);
    }

    public Set<String> getToolNames() {
        return toolMap.keySet();
    }
}
