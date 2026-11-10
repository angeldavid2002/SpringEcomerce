package com.angel.ecomerce.service.order;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface IOrder {
    Order create(Long userId, Long addressId);

    List<Order> getAll();

    List<Order> getByUserId(Long userId);

    List<Order> getByStatus(OrderStatus status);

    Optional<Order> findById(Long id);

    Order addItem(Long orderId, Long productId, int quantity);

    Order updateItemQuantity(Long orderId, Long orderItemId, int quantity);

    Order removeItem(Long orderId, Long orderItemId);

    Order updateStatus(Long orderId, OrderStatus status);

    Order cancel(Long orderId);

    void deleteById(Long id);
}
