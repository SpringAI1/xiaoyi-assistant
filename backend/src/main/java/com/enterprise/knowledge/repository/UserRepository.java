package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
