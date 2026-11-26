package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartsRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
}
