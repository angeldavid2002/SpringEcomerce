package com.angel.ecomerce.dto.product;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean active,
        Long categoryId,
        String categoryName,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static ProductResponse from(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.isActive(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getCreated(),
                product.getUpdated()
        );
    }
}
