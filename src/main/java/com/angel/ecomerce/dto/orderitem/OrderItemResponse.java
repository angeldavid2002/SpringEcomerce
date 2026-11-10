package com.angel.ecomerce.dto.orderitem;

import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.entity.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        Product product = item.getProduct();
        BigDecimal subtotal = item.getUnitPrice() == null
                ? null
                : item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);
        return new OrderItemResponse(
                item.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}
