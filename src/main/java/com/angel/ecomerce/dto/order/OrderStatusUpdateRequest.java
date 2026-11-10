package com.angel.ecomerce.dto.order;

import com.angel.ecomerce.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio")
        OrderStatus status
) {
}
