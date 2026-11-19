package com.angel.ecomerce.support;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.entity.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de prueba reutilizables por los slice tests.
 *
 * <p>Mantener las entidades en un único lugar evita duplicar la construcción
 * de objetos y hace que cada test se concentre en el contrato HTTP.</p>
 */
public final class TestFixtures {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

    private TestFixtures() {
    }

    public static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Descripción de " + name);
        category.setActive(true);
        category.setCreated(FIXED_TIME);
        category.setUpdated(FIXED_TIME);
        return category;
    }

    public static Product product(Long id, String name, String price, Category category) {
        return product(id, name, price, category, true);
    }

    public static Product product(Long id, String name, String price, Category category, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Descripción de " + name);
        product.setPrice(new BigDecimal(price));
        product.setImageUrl("https://example.com/" + id + ".png");
        product.setActive(active);
        product.setCategory(category);
        product.setCreated(FIXED_TIME);
        product.setUpdated(FIXED_TIME);
        return product;
    }

    public static Inventory inventory(Long id, Product product, int stock, int minStock) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setProduct(product);
        inventory.setStock(stock);
        inventory.setMinStock(minStock);
        inventory.setUpdated(FIXED_TIME);
        return inventory;
    }

    public static User user(Long id, String email) {
        return user(id, email, true);
    }

    public static User user(Long id, String email, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName("Cliente");
        user.setLastName("Prueba");
        user.setEmail(email);
        user.setPassword("secreto123");
        user.setRole(Role.CUSTOMER);
        user.setActive(active);
        user.setCreated(FIXED_TIME);
        user.setUpdated(FIXED_TIME);
        return user;
    }

    public static Address address(Long id, User user) {
        Address address = new Address();
        address.setId(id);
        address.setUser(user);
        address.setStreet("Calle 1");
        address.setCity("Ciudad");
        address.setState("Estado");
        address.setZipCode("12345");
        address.setCountry("País");
        return address;
    }

    public static Order order(Long id, User user, Address address, OrderStatus status, String total) {
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal(total));
        order.setCreated(FIXED_TIME);
        order.setUpdated(FIXED_TIME);
        return order;
    }

    public static OrderItem item(Long id, Order order, Product product, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        order.getItems().add(item);
        return item;
    }
}
