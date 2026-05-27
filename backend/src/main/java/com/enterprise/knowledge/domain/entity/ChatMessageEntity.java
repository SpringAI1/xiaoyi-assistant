package com.enterprise.knowledge.domain.entity;

import com.enterprise.knowledge.domain.ChatMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 128, nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private ChatMessage.Role role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "message_order")
    private Integer messageOrder;
}
