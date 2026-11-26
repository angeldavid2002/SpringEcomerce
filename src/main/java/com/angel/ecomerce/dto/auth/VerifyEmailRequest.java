package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición de verificación de email.
 */
public record VerifyEmailRequest(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}
