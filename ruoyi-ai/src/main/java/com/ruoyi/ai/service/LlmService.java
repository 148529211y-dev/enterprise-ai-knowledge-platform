package com.ruoyi.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.domain.dto.ChatMessage;

import java.util.List;

public interface LlmService {

    String chat(String systemPrompt, String userMessage);

    String chat(String systemPrompt, String userMessage, Double temperature);

    String chatWithHistory(List<ChatMessage> messages);

    JSONObject chatWithTools(List<ChatMessage> messages, JSONArray tools);
}
