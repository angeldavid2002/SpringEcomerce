package com.angel.ecomerce.dto.auth;

/**
 * Respuesta de autenticación: token de acceso y (opcional) refresh token.
 */
public record TokenResponse(
        String token,
        String type,
        long expiresIn,
        String refreshToken
) {
    public TokenResponse(String token, String type, long expiresIn) {
        this(token, type, expiresIn, null);
    }

    public static TokenResponse bearer(String token, long expiresIn) {
        return new TokenResponse(token, "Bearer", expiresIn, null);
    }

    public static TokenResponse bearerWithRefresh(String token, String refreshToken, long expiresIn) {
        return new TokenResponse(token, "Bearer", expiresIn, refreshToken);
    }
}
