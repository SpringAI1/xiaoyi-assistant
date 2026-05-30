package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.domain.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档仓储接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Optional<Document> findByIdAndStatusNot(String id, Document.DocumentStatus status);

    List<Document> findByStatus(Document.DocumentStatus status);

    @Query("SELECT d FROM Document d WHERE d.fileName LIKE %:query% OR d.title LIKE %:query%")
    List<Document> searchByFilenameOrTitle(@Param("query") String query);

    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :docId")
    void deleteAllChunksByDocumentId(@Param("docId") String docId);

    long countByUploadedBy(String uploadedBy);
}
