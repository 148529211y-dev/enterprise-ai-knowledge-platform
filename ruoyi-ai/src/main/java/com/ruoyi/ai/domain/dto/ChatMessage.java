package com.ruoyi.ai.domain.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class ChatMessage {

    private String role;
    private String content;
    private JSONArray toolCalls;
    private String toolCallId;
    private String name;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content, JSONArray toolCalls) {
        ChatMessage msg = new ChatMessage("assistant", content);
        msg.setToolCalls(toolCalls);
        return msg;
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        ChatMessage msg = new ChatMessage("tool", content);
        msg.setToolCallId(toolCallId);
        return msg;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("role", role);
        if (content != null) {
            json.put("content", content);
        }
        if (toolCalls != null) {
            json.put("tool_calls", toolCalls);
        }
        if (toolCallId != null) {
            json.put("tool_call_id", toolCallId);
        }
        if (name != null) {
            json.put("name", name);
        }
        return json;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public JSONArray getToolCalls() { return toolCalls; }
    public void setToolCalls(JSONArray toolCalls) { this.toolCalls = toolCalls; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
