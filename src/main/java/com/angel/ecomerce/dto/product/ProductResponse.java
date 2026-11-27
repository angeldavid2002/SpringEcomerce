package com.angel.ecomerce.dto.product;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

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
    /**
     * Mapea el producto. {@code imageUrlResolver} convierte la clave guardada en
     * el bucket en una URL firmada (presigned); si el producto no tiene imagen
     * en el bucket se devuelve la URL externa de siempre.
     */
    public static ProductResponse from(Product product, UnaryOperator<String> imageUrlResolver) {
        Category category = product.getCategory();
        Inventory inventory = product.getInventory();
        Integer stock = inventory == null ? null : inventory.getStock();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getRating(),
                resolveImageUrl(product.getImageKey(), product.getImageUrl(), imageUrlResolver),
                product.isActive(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                stock,
                stock != null && stock > 0,
                product.getCreated(),
                product.getUpdated()
        );
    }

    private static String resolveImageUrl(String imageKey, String legacyUrl, UnaryOperator<String> resolver) {
        if (imageKey != null && !imageKey.isBlank()) {
            return resolver.apply(imageKey);
        }
        return legacyUrl;
    }
}