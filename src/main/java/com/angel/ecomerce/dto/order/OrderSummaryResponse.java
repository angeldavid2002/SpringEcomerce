package com.angel.ecomerce.dto.order;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        Long userId,
        String userName,
        Long addressId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime created,
        LocalDateTime updated
) {
    public static OrderSummaryResponse from(Order order) {
        User user = order.getUser();
        Address address = order.getAddress();
        return new OrderSummaryResponse(
                order.getId(),
                user == null ? null : user.getId(),
                user == null ? null : user.getName(),
                address == null ? null : address.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreated(),
                order.getUpdated()
        );
    }
}
