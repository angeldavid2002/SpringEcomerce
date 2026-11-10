package com.angel.ecomerce.service.orderitem;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;

import java.util.List;
import java.util.Optional;

public interface IOrderItem {
    List<OrderItem> getByOrderId(Long orderId);

    Optional<OrderItem> findById(Long id);

    Order addItem(Long orderId, Long productId, int quantity);

    Order updateQuantity(Long orderId, Long orderItemId, int quantity);

    Order removeItem(Long orderId, Long orderItemId);

    void deleteById(Long orderItemId);
}
