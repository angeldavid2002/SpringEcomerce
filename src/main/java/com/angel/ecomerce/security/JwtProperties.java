package com.angel.ecomerce.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Propiedades del token: secreto HMAC, emisor, expiración y algoritmo.
 *
 * <p>El secreto es obligatorio y su longitud mínima depende del algoritmo:
 * HS256 exige 32 bytes, HS384 48 y HS512 64. Si falta o es corto, la
 * aplicación no arranca y el mensaje lo explica.</p>
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration, MacAlgorithm algorithm) {

    public JwtProperties {
        if (algorithm == null) {
            algorithm = MacAlgorithm.HS256;
        }
        int minimumBytes = switch (algorithm) {
            case HS256 -> 32;
            case HS384 -> 48;
            case HS512 -> 64;
        };
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
            throw new IllegalArgumentException(
                    "app.jwt.secret (JWT_SECRET) es obligatorio y debe tener al menos "
                            + minimumBytes + " bytes para " + algorithm.name());
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
