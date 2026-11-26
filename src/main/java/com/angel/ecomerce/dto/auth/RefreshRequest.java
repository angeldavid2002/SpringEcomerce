package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición de renovación de token. Recibe el refresh token y devuelve un nuevo
 * par access/refresh token.
 */
public record RefreshRequest(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {
}
