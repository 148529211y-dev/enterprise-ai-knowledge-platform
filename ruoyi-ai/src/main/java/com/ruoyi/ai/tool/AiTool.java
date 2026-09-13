package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;

public interface AiTool {

    String execute(String argsJson);

    default JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", new JSONObject());
        return schema;
    }
}
