package com.connectsphere.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.auth.entity.TokenBlacklist;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    
    Optional<TokenBlacklist> findByToken(String token);
    
    boolean existsByToken(String token);
}
