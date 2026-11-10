package com.angel.ecomerce.dto.category;

import com.angel.ecomerce.entity.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreated(),
                category.getUpdated()
        );
    }
}
