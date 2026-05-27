package com.enterprise.knowledge.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户实体 - 用于权限控制
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;

    @Column(length = 200)
    private String email;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "last_login")
    private Instant lastLogin;

    public enum UserRole {
        ADMIN,     // 管理员
        USER,      // 普通用户
        VIEWER     // 只读用户
    }
}
