package com.angel.ecomerce.dto.category;

import com.angel.ecomerce.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description,
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
    public Category toEntity() {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setActive(active);
        return category;
    }
}
