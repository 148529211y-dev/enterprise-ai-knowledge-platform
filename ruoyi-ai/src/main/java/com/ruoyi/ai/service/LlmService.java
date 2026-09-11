package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.vo.ChatMessageVO;

import java.util.List;
import java.util.Map;

/**
 * LLM 调用服务接口
 */
public interface LlmService {

    /** 单轮对话 */
    String chat(String systemPrompt, String userMessage);

    /** 多轮对话（带历史） */
    String chatWithHistory(List<Map<String, String>> messages);

    /** 带工具定义的对话 — Function Calling */
    com.alibaba.fastjson2.JSONObject chatWithTools(List<Map<String, String>> messages,
                                                    com.alibaba.fastjson2.JSONArray tools);
}
