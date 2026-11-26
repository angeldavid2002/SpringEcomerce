package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de restablecimiento de contraseña.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        String token,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 255, message = "La contraseña debe tener entre 6 y 255 caracteres")
        String password
) {
}
