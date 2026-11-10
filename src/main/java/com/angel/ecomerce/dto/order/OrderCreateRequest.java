package com.angel.ecomerce.dto.order;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long userId,
        @NotNull(message = "La dirección es obligatoria")
        Long addressId
) {
}
