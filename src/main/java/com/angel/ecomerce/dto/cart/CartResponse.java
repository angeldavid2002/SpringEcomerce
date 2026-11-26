package com.angel.ecomerce.dto.cart;

import com.angel.ecomerce.entity.Cart;
import com.angel.ecomerce.entity.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del carrito con todas sus líneas y el total.
 */
public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal total,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CartResponse from(Cart cart, List<CartItem> cartItems) {
        List<CartItemResponse> itemResponses = cartItems == null
                ? List.of()
                : cartItems.stream().map(CartItemResponse::from).toList();
        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                cart.getUser() == null ? null : cart.getUser().getId(),
                itemResponses,
                total,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}
