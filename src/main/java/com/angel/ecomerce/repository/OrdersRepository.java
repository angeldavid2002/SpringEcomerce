package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedDesc(Long userId);

    List<Order> findByStatus(OrderStatus status);

    boolean existsByUserId(Long userId);

    boolean existsByAddressId(Long addressId);
}
