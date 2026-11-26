package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemsRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    boolean existsByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);
}
