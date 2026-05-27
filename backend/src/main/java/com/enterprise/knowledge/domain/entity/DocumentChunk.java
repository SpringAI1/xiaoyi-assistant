package com.enterprise.knowledge.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 文档切片实体 - 包含向量嵌入
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_doc_id", columnList = "document_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer chunkNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
