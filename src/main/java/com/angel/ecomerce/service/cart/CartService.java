package com.angel.ecomerce.service.cart;

import com.angel.ecomerce.dto.cart.CartItemResponse;
import com.angel.ecomerce.dto.cart.CartResponse;
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

import java.util.NoSuchElementException;

/**
 * Gestión del carrito de compras de un cliente.
 *
 * <p>Cada cliente tiene un único carrito. Las operaciones son:
 * obtener, añadir/actualizar línea, quitar línea y vaciar.</p>
 *
 * <p>Los métodos devuelven DTOs ya montados: la conversión ocurre dentro de la
 * transacción, así que las relaciones perezosas ({@code product}) se cargan
 * antes de cerrar la sesión y el controlador no recibe entidades.</p>
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
    public CartResponse getCart(Long userId) {
        return toResponse(getOrCreateCart(userId));
    }

    @Transactional
    public CartItemResponse addItem(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        Product product = productsRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));

        if (!product.isActive()) {
            throw new IllegalStateException("El producto no está disponible");
        }

        CartItem item = cartItemsRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + quantity);
                    return existing;
                })
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setProduct(product);
                    created.setQuantity(quantity);
                    return created;
                });

        return CartItemResponse.from(cartItemsRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItem(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemsRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NoSuchElementException("El producto no está en el carrito"));

        item.setQuantity(quantity);
        return CartItemResponse.from(cartItemsRepository.save(item));
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

    private CartResponse toResponse(Cart cart) {
        return CartResponse.from(cart, cartItemsRepository.findByCartId(cart.getId()));
    }

    private Cart getOrCreateCart(Long userId) {
        return cartsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = usersRepository.findById(userId)
                            .orElseThrow(() -> new NoSuchElementException("No existe el usuario"));
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartsRepository.save(cart);
                });
    }
}
