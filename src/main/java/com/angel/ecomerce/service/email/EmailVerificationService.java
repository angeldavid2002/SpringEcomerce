package com.angel.ecomerce.service.email;

import com.angel.ecomerce.entity.EmailVerificationToken;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.repository.EmailVerificationTokenRepository;
import com.angel.ecomerce.repository.UsersRepository;
import com.angel.ecomerce.service.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Genera y valida tokens de verificación de email.
 *
 * <p>En un entorno real el token se enviaría por correo electrónico. Aquí se
 * devuelve en la respuesta para permitir pruebas locales sin servidor de correo.</p>
 */
@Service
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final long TOKEN_EXPIRATION_MINUTES = 60;

    private final UsersRepository usersRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserService userService;

    public EmailVerificationService(UsersRepository usersRepository,
                                    EmailVerificationTokenRepository tokenRepository,
                                    UserService userService) {
        this.usersRepository = usersRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
    }

    @Transactional
    public String createVerificationToken(Long userId) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("El email ya está verificado");
        }

        // Invalidar tokens previos no usados
        tokenRepository.findByUserIdAndUsedFalse(userId)
                .ifPresent(t -> {
                    t.setUsed(true);
                    tokenRepository.save(t);
                });

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(TOKEN_EXPIRATION_MINUTES * 60);

        EmailVerificationToken entry = new EmailVerificationToken();
        entry.setUser(user);
        entry.setToken(token);
        entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        entry.setUsed(false);
        tokenRepository.save(entry);

        return token;
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken entry = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verificación inválido"));

        if (entry.isUsed()) {
            throw new IllegalArgumentException("El token ya fue utilizado");
        }

        Instant expiresAt = entry.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        if (Instant.now().isAfter(expiresAt)) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        User user = entry.getUser();
        user.setEmailVerified(true);
        userService.updateById(user.getId(), user);

        entry.setUsed(true);
        tokenRepository.save(entry);
    }

    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
