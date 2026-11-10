package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.order.OrderCreateRequest;
import com.angel.ecomerce.dto.order.OrderItemAddRequest;
import com.angel.ecomerce.dto.order.OrderItemQuantityRequest;
import com.angel.ecomerce.dto.order.OrderResponse;
import com.angel.ecomerce.dto.order.OrderStatusUpdateRequest;
import com.angel.ecomerce.dto.order.OrderSummaryResponse;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.service.order.IOrder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
public class OrdersController {
    private final IOrder orderService;

    public OrdersController(IOrder orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getAll() {
        return orderService.getAll().stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un pedido con ese id"));
    }

    @GetMapping("/user/{userId}")
    public List<OrderSummaryResponse> getByUser(@PathVariable Long userId) {
        return orderService.getByUserId(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @GetMapping("/status/{status}")
    public List<OrderSummaryResponse> getByStatus(@PathVariable OrderStatus status) {
        return orderService.getByStatus(status).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(orderService.create(request.userId(), request.addressId())));
    }

    @PostMapping("/{orderId}/items")
    public OrderResponse addItem(@PathVariable Long orderId, @Valid @RequestBody OrderItemAddRequest request) {
        return OrderResponse.from(orderService.addItem(orderId, request.productId(), request.quantity()));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    public OrderResponse updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemQuantityRequest request) {
        return OrderResponse.from(orderService.updateItemQuantity(orderId, itemId, request.quantity()));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public OrderResponse removeItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        return OrderResponse.from(orderService.removeItem(orderId, itemId));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return OrderResponse.from(orderService.updateStatus(orderId, request.status()));
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return OrderResponse.from(orderService.cancel(orderId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId) {
        orderService.deleteById(orderId);
        return ResponseEntity.noContent().build();
    }
}
