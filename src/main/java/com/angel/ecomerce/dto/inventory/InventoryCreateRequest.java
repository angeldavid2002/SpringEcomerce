package com.angel.ecomerce.dto.inventory;

import com.angel.ecomerce.entity.Inventory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateRequest(
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,
        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer minStock
) {
    public Inventory toEntity() {
        Inventory inventory = new Inventory();
        inventory.setStock(stock);
        inventory.setMinStock(minStock);
        return inventory;
    }
}
