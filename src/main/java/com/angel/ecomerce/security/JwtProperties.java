package com.angel.ecomerce.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Propiedades del token: secreto HS256, emisor y expiración.
 *
 * <p>El secreto es obligatorio y debe tener al menos 32 bytes para HS256.
 * Si falta o es corto, la aplicación no arranca y el mensaje lo explica.</p>
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret (JWT_SECRET) es obligatorio y debe tener al menos 32 bytes");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.jwt.issuer es obligatorio");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt.expiration debe ser una duración positiva");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
