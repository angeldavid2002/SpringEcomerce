package com.angel.ecomerce.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de añadir o actualizar una línea en el carrito.
 */
public record CartItemRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer quantity
) {
}
