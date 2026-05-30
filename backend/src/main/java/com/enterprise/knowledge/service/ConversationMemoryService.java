package com.enterprise.knowledge.service;

import com.enterprise.knowledge.domain.ChatMessage;
import com.enterprise.knowledge.domain.entity.ChatMessageEntity;
import com.enterprise.knowledge.domain.entity.Conversation;
import com.enterprise.knowledge.repository.ChatMessageRepository;
import com.enterprise.knowledge.repository.ConversationRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatLanguageModel chatModel;
    
    // 7天过期时间
    private static final Duration SESSION_EXPIRATION_DAYS = Duration.ofDays(7);
    private static final int MAX_HISTORY_SIZE = 50;
    private static final int SUMMARY_TRIGGER_THRESHOLD = 20;

    public ConversationMemoryService(ConversationRepository conversationRepository,
                                     ChatMessageRepository chatMessageRepository,
                                     ChatLanguageModel chatModel) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatModel = chatModel;
    }

    @Transactional
    public String createSession() {
        return createSession(null);
    }

    @Transactional
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setTitle("新会话");
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setExpiresAt(Instant.now().plus(SESSION_EXPIRATION_DAYS));
        conversation.setMessageCount(0);
        conversation.setIsActive(true);
        
        conversationRepository.save(conversation);
        return sessionId;
    }

    @Transactional
    public void addMessage(String sessionId, ChatMessage message) {
        // 获取或创建会话
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setSessionId(sessionId);
                    newConv.setTitle("新会话");
                    newConv.setCreatedAt(Instant.now());
                    newConv.setExpiresAt(Instant.now().plus(SESSION_EXPIRATION_DAYS));
                    newConv.setMessageCount(0);
                    newConv.setIsActive(true);
                    return conversationRepository.save(newConv);
                });

        // 更新会话信息
        conversation.setUpdatedAt(Instant.now());
        conversation.setExpiresAt(Instant.now().plus(SESSION_EXPIRATION_DAYS)); // 每次消息重置过期时间
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        
        // 如果是第一条用户消息，用它作为标题
        if (conversation.getMessageCount() == 1 && message.getRole() == ChatMessage.Role.USER) {
            String title = message.getContent().length() > 50 
                    ? message.getContent().substring(0, 50) + "..." 
                    : message.getContent();
            conversation.setTitle(title);
        }
        
        conversationRepository.save(conversation);

        // 保存消息
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setSessionId(sessionId);
        messageEntity.setRole(message.getRole());
        messageEntity.setContent(message.getContent());
        messageEntity.setMessageOrder(conversation.getMessageCount());
        messageEntity.setCreatedAt(Instant.now());
        chatMessageRepository.save(messageEntity);

        // 检查是否需要生成摘要
        if (conversation.getMessageCount() % SUMMARY_TRIGGER_THRESHOLD == 0) {
            generateSummaryAsync(sessionId);
        }
    }

    public List<ChatMessage> getHistory(String sessionId) {
        List<ChatMessageEntity> messageEntities = chatMessageRepository.findBySessionIdOrderByMessageOrderAsc(sessionId);
        
        // 如果超出最大历史记录数，只保留最新的
        if (messageEntities.size() > MAX_HISTORY_SIZE) {
            messageEntities = messageEntities.subList(
                    messageEntities.size() - MAX_HISTORY_SIZE, 
                    messageEntities.size()
            );
        }
        
        return messageEntities.stream()
                .map(this::toChatMessage)
                .collect(Collectors.toList());
    }

    public String buildHistoryPrompt(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        Conversation conversation = conversationRepository.findBySessionId(sessionId).orElse(null);
        String summary = conversation != null ? conversation.getSummary() : null;
        
        StringBuilder prompt = new StringBuilder();
        
        if (summary != null && !summary.isEmpty()) {
            prompt.append("【对话摘要】\n").append(summary).append("\n\n");
        }
        
        if (!history.isEmpty()) {
            prompt.append("【历史对话】\n\n");
            
            int startIndex = Math.max(0, history.size() - 10);
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                String role = switch (msg.getRole()) {
                    case USER -> "用户";
                    case ASSISTANT -> "助手";
                    case SYSTEM -> "系统";
                    case TOOL -> "工具";
                };
                prompt.append(role).append("：").append(msg.getContent()).append("\n\n");
            }
        }
        
        return prompt.toString();
    }

    private void generateSummaryAsync(String sessionId) {
        new Thread(() -> {
            try {
                generateSummary(sessionId);
            } catch (Exception e) {
                System.err.println("Failed to generate summary: " + e.getMessage());
            }
        }).start();
    }

    @Transactional
    public String generateSummary(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        if (history.size() < 5) {
            return null;
        }
        
        StringBuilder fullHistory = new StringBuilder();
        for (ChatMessage msg : history) {
            String role = switch (msg.getRole()) {
                case USER -> "用户";
                case ASSISTANT -> "助手";
                case SYSTEM -> "系统";
                case TOOL -> "工具";
            };
            fullHistory.append(role).append("：").append(msg.getContent()).append("\n\n");
        }
        
        String prompt = """
            请为以下对话生成一个简洁的摘要（不超过300字），保留关键信息和话题脉络：
            
            %s
            
            摘要：
            """.formatted(fullHistory.toString());
        
        try {
            String summary = chatModel.generate(prompt);
            Conversation conversation = conversationRepository.findBySessionId(sessionId).orElse(null);
            if (conversation != null) {
                conversation.setSummary(summary);
                conversationRepository.save(conversation);
            }
            return summary;
        } catch (Exception e) {
            System.err.println("Summary generation failed: " + e.getMessage());
            return null;
        }
    }

    @Transactional
    public void clearSession(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        conversationRepository.deactivateSession(sessionId);
    }

    @Transactional
    public void clearAllSessions() {
        List<Conversation> conversations = conversationRepository.findAll();
        for (Conversation conv : conversations) {
            chatMessageRepository.deleteBySessionId(conv.getSessionId());
        }
        conversationRepository.deleteAll();
    }

    public int getSessionCount() {
        return (int) conversationRepository.count();
    }

    public boolean hasSession(String sessionId) {
        return conversationRepository.findBySessionId(sessionId).isPresent();
    }

    public String getSummary(String sessionId) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId).orElse(null);
        return conversation != null ? conversation.getSummary() : null;
    }

    public List<Conversation> getUserSessions(String userId) {
        return conversationRepository.findByUserIdAndIsActiveOrderByUpdatedAtDesc(userId, true);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredSessions() {
        int deleted = conversationRepository.deleteExpiredSessions(Instant.now());
        System.out.println("Cleaned up " + deleted + " expired sessions");
    }

    private ChatMessage toChatMessage(ChatMessageEntity entity) {
        ChatMessage message = new ChatMessage();
        message.setRole(entity.getRole());
        message.setContent(entity.getContent());
        return message;
    }
}
