package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.cart.CartItemRequest;
import com.angel.ecomerce.dto.cart.CartItemResponse;
import com.angel.ecomerce.dto.cart.CartResponse;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.cart.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    private final OwnershipGuard ownership;

    public CartController(CartService cartService, OwnershipGuard ownership) {
        this.cartService = cartService;
        this.ownership = ownership;
    }

    /**
     * Obtiene el carrito del usuario autenticado con todas sus líneas y el total.
     */
    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart(ownership.currentUserId());
    }

    /**
     * Añade o actualiza una línea en el carrito.
     * Cuerpo: { "productId": 1, "quantity": 2 }
     */
    @PostMapping
    public CartItemResponse addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(ownership.currentUserId(), request.productId(), request.quantity());
    }

    /**
     * Actualiza la cantidad de un producto ya en el carrito.
     * Cuerpo: { "productId": 1, "quantity": 3 }
     */
    @PutMapping
    public CartItemResponse updateItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.updateItem(ownership.currentUserId(), request.productId(), request.quantity());
    }

    /**
     * Quita un producto del carrito. Es idempotente: si no estaba, responde 204.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long productId) {
        cartService.removeItem(ownership.currentUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vacía completamente el carrito del usuario autenticado.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clear(ownership.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
