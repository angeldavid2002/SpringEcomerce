package com.angel.ecomerce.dto.product;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal rating,
        String imageUrl,
        boolean active,
        Long categoryId,
        String categoryName,
        Integer stock,
        boolean available,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static ProductResponse from(Product product) {
        Category category = product.getCategory();
        Inventory inventory = product.getInventory();
        Integer stock = inventory == null ? null : inventory.getStock();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getRating(),
                product.getImageUrl(),
                product.isActive(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                stock,
                stock != null && stock > 0,
                product.getCreated(),
                product.getUpdated()
        );
    }
}
