package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserIdAndUsedFalse(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
