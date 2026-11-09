package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}
