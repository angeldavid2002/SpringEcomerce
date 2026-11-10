package com.angel.ecomerce.dto.category;

import com.angel.ecomerce.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description
) {
    public Category toEntity() {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return category;
    }
}
