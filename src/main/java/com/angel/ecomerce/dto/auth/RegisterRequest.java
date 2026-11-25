package com.angel.ecomerce.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta pública de un cliente. No incluye rol a propósito: el servicio siempre
 * crea un {@code CUSTOMER} para evitar la escalada de privilegios.
 */
public record RegisterRequest(
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "El apellido no puede superar los 255 caracteres")
        String lastName,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar los 255 caracteres")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 255, message = "La contraseña debe tener entre 6 y 255 caracteres")
        String password
) {
}
