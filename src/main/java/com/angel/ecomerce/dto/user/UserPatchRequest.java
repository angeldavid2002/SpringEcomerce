package com.angel.ecomerce.dto.user;

import com.angel.ecomerce.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Petición de actualización parcial del perfil autenticado (PATCH /users/me).
 * Todos los campos son opcionales: solo se actualiza el que llega. No se
 * permite cambiar la contraseña ni el rol desde este endpoint.
 */
public record UserPatchRequest(
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "El apellido no puede superar los 255 caracteres")
        String lastName,
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar los 255 caracteres")
        String email
) {
    public void applyTo(User user) {
        if (name != null) {
            user.setName(name.trim());
        }
        if (lastName != null) {
            user.setLastName(lastName.trim());
        }
        if (hasEmail()) {
            user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        }
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
