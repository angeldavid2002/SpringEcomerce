package com.angel.ecomerce.dto.category;

import com.angel.ecomerce.entity.Category;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        boolean active,
        LocalDateTime created,
        LocalDateTime updated
) {
    /**
     * Mapea la categoría. {@code imageUrlResolver} convierte la clave guardada
     * en el bucket en una URL firmada (presigned); si la categoría no tiene
     * imagen se devuelve {@code null}.
     */
    public static CategoryResponse from(Category category, UnaryOperator<String> imageUrlResolver) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageKey() == null || category.getImageKey().isBlank()
                        ? null
                        : imageUrlResolver.apply(category.getImageKey()),
                category.isActive(),
                category.getCreated(),
                category.getUpdated()
        );
    }
}