package com.angel.ecomerce.service.cart;

import com.angel.ecomerce.entity.Cart;
import com.angel.ecomerce.entity.CartItem;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.repository.CartItemsRepository;
import com.angel.ecomerce.repository.CartsRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Gestión del carrito de compras de un cliente.
 *
 * <p>Cada cliente tiene un único carrito. Las operaciones son:
 * obtener, añadir/actualizar línea, quitar línea y vaciar.</p>
 */
@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartsRepository cartsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    public CartService(CartsRepository cartsRepository,
                       CartItemsRepository cartItemsRepository,
                       ProductsRepository productsRepository,
                       UsersRepository usersRepository) {
        this.cartsRepository = cartsRepository;
        this.cartItemsRepository = cartItemsRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartsRepository.findByUserId(userId)
                .map(cart -> {
                    // Forzar carga de items
                    cart.getItems().size();
                    return cart;
                })
                .orElseGet(() -> {
                    User user = usersRepository.findById(userId)
                            .orElseThrow(() -> new NoSuchElementException("No existe el usuario"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartsRepository.save(cart);
                });
    }

    @Transactional
    public CartItem addItem(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        Product product = productsRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));

        if (!product.isActive()) {
            throw new IllegalStateException("El producto no está disponible");
        }

        return cartItemsRepository.findByCartId(cart.getId()).stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + quantity);
                    return cartItemsRepository.save(existing);
                })
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setCart(cart);
                    item.setProduct(product);
                    item.setQuantity(quantity);
                    return cartItemsRepository.save(item);
                });
    }

    @Transactional
    public CartItem updateItem(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        return cartItemsRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(item -> {
                    item.setQuantity(quantity);
                    return cartItemsRepository.save(item);
                })
                .orElseThrow(() -> new NoSuchElementException(
                        "El producto no está en el carrito"));
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        cartItemsRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresent(cartItemsRepository::delete);
    }

    @Transactional
    public void clear(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemsRepository.deleteByCartId(cart.getId());
    }

    public List<CartItem> getItems(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartItemsRepository.findByCartId(cart.getId());
    }
}
