package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de recuperación de contraseña. Recibe el email y devuelve el token
 * de restablecimiento (en producción se enviaría por correo).
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email
) {
}
