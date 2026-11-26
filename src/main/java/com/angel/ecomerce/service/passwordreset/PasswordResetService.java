package com.angel.ecomerce.service.passwordreset;

import com.angel.ecomerce.entity.PasswordResetToken;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.repository.PasswordResetTokenRepository;
import com.angel.ecomerce.repository.UsersRepository;
import com.angel.ecomerce.service.users.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Genera, valida y consume tokens de recuperación de contraseña.
 *
 * <p>El token es de un solo uso y expira tras un tiempo corto. Una vez usado
 * para restablecer la contraseña se marca como usado y no se puede reutilizar.</p>
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final UsersRepository usersRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UsersRepository usersRepository,
                                PasswordResetTokenRepository tokenRepository,
                                UserService userService,
                                PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String createResetToken(String email) {
        User user = usersRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("No existe un usuario activo con ese email"));

        // Invalidar tokens previos no usados de este usuario
        tokenRepository.findByUserIdAndUsedFalse(user.getId())
                .ifPresent(t -> {
                    t.setUsed(true);
                    tokenRepository.save(t);
                });

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(TOKEN_EXPIRATION_MINUTES * 60);

        PasswordResetToken entry = new PasswordResetToken();
        entry.setUser(user);
        entry.setToken(token);
        entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        entry.setUsed(false);
        tokenRepository.save(entry);

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken reset = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperación inválido"));

        if (reset.isUsed()) {
            throw new IllegalArgumentException("El token ya fue utilizado");
        }

        Instant expiresAt = reset.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        if (Instant.now().isAfter(expiresAt)) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        User user = reset.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        reset.setUsed(true);
        tokenRepository.save(reset);
    }

    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
