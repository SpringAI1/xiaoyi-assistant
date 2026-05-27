package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    Optional<Conversation> findBySessionId(String sessionId);

    List<Conversation> findByUserIdAndIsActiveOrderByUpdatedAtDesc(String userId, Boolean isActive);

    @Modifying
    @Query("UPDATE Conversation c SET c.isActive = false WHERE c.sessionId = :sessionId")
    void deactivateSession(@Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.expiresAt < :now AND c.isActive = false")
    int deleteExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Conversation c SET c.updatedAt = :now WHERE c.sessionId = :sessionId")
    void updateLastActivity(@Param("sessionId") String sessionId, @Param("now") Instant now);
}
