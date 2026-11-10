package com.angel.ecomerce.dto.user;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String lastName,
        String email,
        Role role,
        boolean active,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreated(),
                user.getUpdated()
        );
    }
}
