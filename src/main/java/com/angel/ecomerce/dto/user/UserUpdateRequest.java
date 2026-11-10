package com.angel.ecomerce.dto.user;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "El apellido no puede superar los 255 caracteres")
        String lastName,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar los 255 caracteres")
        String email,
        @Size(min = 6, max = 255, message = "La contraseña debe tener entre 6 y 255 caracteres")
        String password,
        Role role,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
    public User toEntity() {
        User user = new User();
        user.setName(name);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
