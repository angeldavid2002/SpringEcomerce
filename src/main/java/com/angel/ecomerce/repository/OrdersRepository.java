package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Order, Long> {
    boolean existsByUserId(Long userId);

    boolean existsByAddressId(Long addressId);

    List<Order> findAllByOrderByCreatedDescIdDesc();

    List<Order> findByUserIdOrderByCreatedDescIdDesc(Long userId);

    List<Order> findByStatusOrderByCreatedDescIdDesc(OrderStatus status);
}
