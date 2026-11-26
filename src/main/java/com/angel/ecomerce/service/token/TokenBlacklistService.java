package com.angel.ecomerce.service.token;

import com.angel.ecomerce.repository.TokenBlacklistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Operaciones sobre la lista negra de JWT revocados.
 */
@Service
@Transactional(readOnly = true)
public class TokenBlacklistService {

    private final TokenBlacklistRepository blacklistRepository;

    public TokenBlacklistService(TokenBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Transactional
    public void revoke(String jti, LocalDateTime expiresAt) {
        if (jti != null && !blacklistRepository.findByJti(jti).isPresent()) {
            com.angel.ecomerce.entity.TokenBlacklist entry = new com.angel.ecomerce.entity.TokenBlacklist();
            entry.setJti(jti);
            entry.setExpiresAt(expiresAt);
            blacklistRepository.save(entry);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && blacklistRepository.findByJti(jti).isPresent();
    }

    @Transactional
    public void purgeExpired() {
        blacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
