package com.angel.ecomerce.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemAddRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productId,
        @Positive(message = "La cantidad debe ser mayor a cero")
        int quantity
) {
}
