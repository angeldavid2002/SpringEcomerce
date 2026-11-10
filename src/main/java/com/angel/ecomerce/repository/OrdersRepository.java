package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Order, Long> {
    boolean existsByUserId(Long userId);

    boolean existsByAddressId(Long addressId);

    @EntityGraph(attributePaths = "user")
    List<Order> findAllByOrderByCreatedDescIdDesc();

    @EntityGraph(attributePaths = "user")
    List<Order> findByUserIdOrderByCreatedDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Order> findByStatusOrderByCreatedDescIdDesc(OrderStatus status);

    @Override
    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<Order> findById(Long id);
}
