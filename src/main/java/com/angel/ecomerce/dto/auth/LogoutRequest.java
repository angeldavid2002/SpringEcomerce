package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición de cierre de sesión. Recibe el JWT actual para agregarlo a la
 * lista negra de tokens revocados.
 */
public record LogoutRequest(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}
