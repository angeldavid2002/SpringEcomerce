package com.angel.ecomerce.dto.inventory;

import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
        String productName,
        int stock,
        int minStock,
        boolean lowStock,
        LocalDateTime updated
) {
    public static InventoryResponse from(Inventory inventory) {
        Product product = inventory.getProduct();
        return new InventoryResponse(
                inventory.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                inventory.getStock(),
                inventory.getMinStock(),
                inventory.getStock() <= inventory.getMinStock(),
                inventory.getUpdated()
        );
    }
}
