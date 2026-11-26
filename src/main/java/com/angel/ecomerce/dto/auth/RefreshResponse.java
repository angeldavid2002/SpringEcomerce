package com.angel.ecomerce.dto.auth;

/**
 * Respuesta de renovación de token.
 */
public record RefreshResponse(
        String token,
        String refreshToken,
        String type,
        long expiresIn
) {
    public static RefreshResponse bearer(String token, String refreshToken, long expiresIn) {
        return new RefreshResponse(token, refreshToken, "Bearer", expiresIn);
    }
}
