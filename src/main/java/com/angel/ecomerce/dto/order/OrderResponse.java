package com.angel.ecomerce.dto.order;

import com.angel.ecomerce.dto.orderitem.OrderItemResponse;
import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String userName,
        Long addressId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime created,
        LocalDateTime updated,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        User user = order.getUser();
        Address address = order.getAddress();
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(OrderItemResponse::from).toList();
        return new OrderResponse(
                order.getId(),
                user == null ? null : user.getId(),
                user == null ? null : user.getName(),
                address == null ? null : address.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreated(),
                order.getUpdated(),
                items
        );
    }
}
