package com.angel.ecomerce.service.order;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.repository.AddressesRepository;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.repository.UsersRepository;
import com.angel.ecomerce.service.inventory.IInventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrderService implements IOrder {
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final AddressesRepository addressesRepository;
    private final ProductsRepository productsRepository;
    private final IInventory inventoryService;

    public OrderService(OrdersRepository ordersRepository,
                        UsersRepository usersRepository,
                        AddressesRepository addressesRepository,
                        ProductsRepository productsRepository,
                        IInventory inventoryService) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.addressesRepository = addressesRepository;
        this.productsRepository = productsRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public Order create(Long userId, Long addressId) {
        if (userId == null || addressId == null) {
            throw new IllegalArgumentException("El usuario y la dirección son obligatorios");
        }
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No existe el usuario con id " + userId));
        if (!user.isActive()) {
            throw new IllegalArgumentException("El usuario está inactivo y no puede crear pedidos");
        }
        Address address = addressesRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe la dirección con id " + addressId + " para el usuario " + userId));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(money(BigDecimal.ZERO));
        return ordersRepository.save(order);
    }

    @Override
    public List<Order> getAll() {
        return ordersRepository.findAllByOrderByCreatedDescIdDesc();
    }

    @Override
    public List<Order> getByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        return ordersRepository.findByUserIdOrderByCreatedDescIdDesc(userId);
    }

    @Override
    public List<Order> getByStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio");
        }
        return ordersRepository.findByStatusOrderByCreatedDescIdDesc(status);
    }

    @Override
    public Optional<Order> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return ordersRepository.findById(id);
    }

    @Override
    @Transactional
    public Order addItem(Long orderId, Long productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        Order order = getPendingOrder(orderId);
        Product product = productsRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("No existe el producto con id " + productId));
        if (!product.isActive()) {
            throw new IllegalArgumentException("El producto no está activo");
        }
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("El producto no tiene precio definido");
        }

        OrderItem item = order.getItems().stream()
                .filter(existing -> existing.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
            item.setQuantity(quantity);
            order.getItems().add(item);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }

        recalculateTotal(order);
        return ordersRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateItemQuantity(Long orderId, Long orderItemId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        if (orderItemId == null) {
            throw new IllegalArgumentException("El item del pedido es obligatorio");
        }

        Order order = getPendingOrder(orderId);
        OrderItem item = getItem(order, orderItemId);

        if (quantity == 0) {
            order.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        recalculateTotal(order);
        return ordersRepository.save(order);
    }

    @Override
    @Transactional
    public Order removeItem(Long orderId, Long orderItemId) {
        if (orderItemId == null) {
            throw new IllegalArgumentException("El item del pedido es obligatorio");
        }

        Order order = getPendingOrder(orderId);
        OrderItem item = getItem(order, orderItemId);

        order.getItems().remove(item);
        recalculateTotal(order);
        return ordersRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio");
        }

        Order order = getOrder(orderId);

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == status) {
            return order;
        }
        if (!allowedTransitions(currentStatus).contains(status)) {
            throw new IllegalArgumentException(
                    "Transición de estado no permitida: " + currentStatus + " -> " + status);
        }

        if (status == OrderStatus.PAID) {
            reserveStock(order);
        }
        if (currentStatus == OrderStatus.PAID && status == OrderStatus.CANCELLED) {
            releaseStock(order);
        }

        order.setStatus(status);
        return ordersRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancel(Long orderId) {
        return updateStatus(orderId, OrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del pedido es obligatorio");
        }

        Order order = ordersRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe un pedido con ese id"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden eliminar pedidos en estado PENDING");
        }

        ordersRepository.delete(order);
    }

    private Order getPendingOrder(Long orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden modificar pedidos en estado PENDING");
        }
        return order;
    }

    private Order getOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("El id del pedido es obligatorio");
        }
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("No existe el pedido con id " + orderId));
    }

    private OrderItem getItem(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe el item con id " + orderItemId + " en el pedido " + order.getId()));
    }

    private void recalculateTotal(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(money(total));
    }

    private void reserveStock(Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede pagar un pedido sin productos");
        }

        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryService.findByProductId(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay inventario para el producto " + item.getProduct().getName()));
            if (inventory.getStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Stock insuficiente para el producto " + item.getProduct().getName());
            }
        }

        order.getItems().forEach(item ->
                inventoryService.decreaseStock(item.getProduct().getId(), item.getQuantity()));
    }

    private void releaseStock(Order order) {
        order.getItems().forEach(item ->
                inventoryService.increaseStock(item.getProduct().getId(), item.getQuantity()));
    }

    private Set<OrderStatus> allowedTransitions(OrderStatus status) {
        return switch (status) {
            case PENDING -> EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED);
            case PAID -> EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
            case SHIPPED -> EnumSet.of(OrderStatus.DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
