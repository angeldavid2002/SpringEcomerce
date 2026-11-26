package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    Optional<TokenBlacklist> findByJti(String jti);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
