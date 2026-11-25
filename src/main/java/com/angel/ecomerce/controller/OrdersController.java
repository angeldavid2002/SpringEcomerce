package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.order.OrderCreateRequest;
import com.angel.ecomerce.dto.order.OrderItemAddRequest;
import com.angel.ecomerce.dto.order.OrderItemQuantityRequest;
import com.angel.ecomerce.dto.order.OrderResponse;
import com.angel.ecomerce.dto.order.OrderStatusUpdateRequest;
import com.angel.ecomerce.dto.order.OrderSummaryResponse;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.security.OwnershipGuard;
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
    private final OwnershipGuard ownership;

    public OrdersController(IOrder orderService, OwnershipGuard ownership) {
        this.orderService = orderService;
        this.ownership = ownership;
    }

    @GetMapping
    public List<OrderSummaryResponse> getAll() {
        return orderService.getAll().stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @GetMapping("/me")
    public List<OrderSummaryResponse> getMyOrders() {
        Long userId = ownership.currentUserId();
        return orderService.getByUserId(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return OrderResponse.from(requireOwnedOrder(id));
    }

    @GetMapping("/user/{userId}")
    public List<OrderSummaryResponse> getByUser(@PathVariable Long userId) {
        ownership.requireSelfOrAdmin(userId);
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
        ownership.requireSelfOrAdmin(request.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(orderService.create(request.userId(), request.addressId())));
    }

    @PostMapping("/{orderId}/items")
    public OrderResponse addItem(@PathVariable Long orderId, @Valid @RequestBody OrderItemAddRequest request) {
        requireOwnedOrder(orderId);
        return OrderResponse.from(orderService.addItem(orderId, request.productId(), request.quantity()));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    public OrderResponse updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemQuantityRequest request) {
        requireOwnedOrder(orderId);
        return OrderResponse.from(orderService.updateItemQuantity(orderId, itemId, request.quantity()));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public OrderResponse removeItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        requireOwnedOrder(orderId);
        return OrderResponse.from(orderService.removeItem(orderId, itemId));
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return OrderResponse.from(orderService.updateStatus(orderId, request.status()));
    }

    @PostMapping("/{orderId}/pay")
    public OrderResponse pay(@PathVariable Long orderId) {
        requireOwnedOrder(orderId);
        return OrderResponse.from(orderService.pay(orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        requireOwnedOrder(orderId);
        return OrderResponse.from(orderService.cancel(orderId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId) {
        orderService.deleteById(orderId);
        return ResponseEntity.noContent().build();
    }

    private Order requireOwnedOrder(Long orderId) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("No existe un pedido con ese id"));
        ownership.requireOwnerOrAdmin(order.getUser().getId());
        return order;
    }
}
