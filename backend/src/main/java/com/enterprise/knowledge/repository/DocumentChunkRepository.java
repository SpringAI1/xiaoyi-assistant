package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.domain.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档切片仓储接口 - 包含向量检索
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    @Query(value = "SELECT dc.*, cosine_similarity(dc.embedding, :queryVector) AS similarity " +
            "FROM document_chunks dc " +
            "WHERE dc.embedding IS NOT NULL " +
            "ORDER BY dc.embedding <=> :queryVector " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarVectors(
            @Param("queryVector") float[] queryVector,
            @Param("limit") int limit);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :docId ORDER BY dc.chunkNumber")
    List<DocumentChunk> findByDocumentId(@Param("docId") String docId);

    Optional<DocumentChunk> findByIdAndDocumentId(String id, String documentId);

    long countByDocumentId(String documentId);
}
