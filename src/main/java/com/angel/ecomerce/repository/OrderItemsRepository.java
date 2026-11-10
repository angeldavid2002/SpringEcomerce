package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemsRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);

    @EntityGraph(attributePaths = "product")
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Override
    @EntityGraph(attributePaths = "product")
    Optional<OrderItem> findById(Long id);
}
