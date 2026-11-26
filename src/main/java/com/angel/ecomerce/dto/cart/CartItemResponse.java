package com.angel.ecomerce.dto.cart;

import com.angel.ecomerce.entity.CartItem;
import com.angel.ecomerce.entity.Product;

import java.math.BigDecimal;

/**
 * Línea de carrito con datos del producto.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal productPrice,
        Integer quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        BigDecimal price = product == null ? BigDecimal.ZERO : product.getPrice();
        return new CartItemResponse(
                item.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                price,
                item.getQuantity(),
                price.multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
