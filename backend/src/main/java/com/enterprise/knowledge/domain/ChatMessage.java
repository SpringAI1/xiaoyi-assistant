package com.enterprise.knowledge.domain;

import java.time.Instant;

/**
 * 对话消息
 */
public class ChatMessage {
    public enum Role {
        USER,        // 用户
        ASSISTANT,   // AI 助手
        SYSTEM,      // 系统
        TOOL         // 工具调用
    }

    private String id;
    private Role role;
    private String content;
    private Instant timestamp;
    private String toolCallId; // 工具调用 ID
    private Object toolResult; // 工具执行结果

    public ChatMessage() {}

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public Object getToolResult() { return toolResult; }
    public void setToolResult(Object toolResult) { this.toolResult = toolResult; }
}
