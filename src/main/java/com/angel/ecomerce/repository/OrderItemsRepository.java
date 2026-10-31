package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemsRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    boolean existsByProductId(Long productId);
}
