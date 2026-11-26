package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.cart.CartItemRequest;
import com.angel.ecomerce.dto.cart.CartItemResponse;
import com.angel.ecomerce.dto.cart.CartResponse;
import com.angel.ecomerce.entity.Cart;
import com.angel.ecomerce.entity.CartItem;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.cart.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

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
        Long userId = ownership.currentUserId();
        Cart cart = cartService.getOrCreateCart(userId);
        return CartResponse.from(cart);
    }

    /**
     * Añade o actualiza una línea en el carrito.
     * Cuerpo: { "productId": 1, "quantity": 2 }
     */
    @PostMapping
    public CartItemResponse addItem(@RequestBody CartItemRequest request) {
        Long userId = ownership.currentUserId();
        CartItem item = cartService.addItem(userId, request.productId(), request.quantity());
        return CartItemResponse.from(item);
    }

    /**
     * Actualiza la cantidad de un producto ya en el carrito.
     * Cuerpo: { "productId": 1, "quantity": 3 }
     */
    @PutMapping
    public CartItemResponse updateItem(@RequestBody CartItemRequest request) {
        Long userId = ownership.currentUserId();
        CartItem item = cartService.updateItem(userId, request.productId(), request.quantity());
        return CartItemResponse.from(item);
    }

    /**
     * Quita un producto del carrito.
     * Parámetro query: ?productId=1
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long productId) {
        Long userId = ownership.currentUserId();
        cartService.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vacía completamente el carrito del usuario autenticado.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        Long userId = ownership.currentUserId();
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
