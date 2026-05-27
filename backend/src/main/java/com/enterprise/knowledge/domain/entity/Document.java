package com.enterprise.knowledge.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档实体 - 持久化存储
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private String id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocumentStatus status = DocumentStatus.PROCESSING;

    private Integer version = 1;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // 扩展元数据
    @ElementCollection
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @CollectionTable(name = "document_metadata")
    private Map<String, String> metadata = new HashMap<>();

    // 关联的切片
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<DocumentChunk> chunks = new java.util.ArrayList<>();

    public enum DocumentStatus {
        PROCESSING,   // 处理中
        DONE,         // 完成
        FAILED,       // 失败
        DELETED       // 已删除
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
