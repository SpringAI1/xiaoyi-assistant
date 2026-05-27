package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.domain.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySessionIdOrderByMessageOrderAsc(String sessionId);

    @Modifying
    @Query("DELETE FROM ChatMessageEntity m WHERE m.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.sessionId = :sessionId")
    int countBySessionId(@Param("sessionId") String sessionId);
}
