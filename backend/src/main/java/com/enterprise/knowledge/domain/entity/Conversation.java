package com.enterprise.knowledge.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
