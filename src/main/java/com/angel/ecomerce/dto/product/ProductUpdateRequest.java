package com.angel.ecomerce.dto.product;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener como máximo 10 enteros y 2 decimales")
        BigDecimal price,
        @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
        String imageUrl,
        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
    public Product toEntity() {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        Category category = new Category();
        category.setId(categoryId);
        product.setCategory(category);
        product.setActive(active);
        return product;
    }
}
