package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
