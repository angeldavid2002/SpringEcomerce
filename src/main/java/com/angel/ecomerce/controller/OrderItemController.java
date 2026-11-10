package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.orderitem.OrderItemResponse;
import com.angel.ecomerce.service.orderitem.IOrderItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/order-items")
public class OrderItemController {
    private final IOrderItem orderItemService;

    public OrderItemController(IOrderItem orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping("/{id}")
    public OrderItemResponse getById(@PathVariable Long id) {
        return orderItemService.findById(id)
                .map(OrderItemResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un item de pedido con ese id"));
    }

    @GetMapping
    public List<OrderItemResponse> getByOrder(@RequestParam Long orderId) {
        return orderItemService.getByOrderId(orderId).stream()
                .map(OrderItemResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderItemService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
