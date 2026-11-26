package com.angel.ecomerce.config;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.AddressesRepository;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.InventoriesRepository;
import com.angel.ecomerce.repository.OrderItemsRepository;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.repository.UsersRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea datos de demostración al arrancar la aplicación si aún no existen.
 *
 * <p>Es idempotente y se puede desactivar con {@code app.seed.enabled=false}.
 * Crea: 2 usuarios (admin y customer), 5 categorías, 10 productos con su
 * inventario, 2 direcciones para el customer y 1 pedido con items.
 * Las credenciales vienen de {@link SeedProperties} (variables de entorno o
 * .env), se hashean y nunca se registran en el log.</p>
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UsersRepository usersRepository;
    private final CategoriesRepository categoriesRepository;
    private final ProductsRepository productsRepository;
    private final InventoriesRepository inventoriesRepository;
    private final AddressesRepository addressesRepository;
    private final OrdersRepository ordersRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seed;

    public DemoDataSeeder(UsersRepository usersRepository,
                          CategoriesRepository categoriesRepository,
                          ProductsRepository productsRepository,
                          InventoriesRepository inventoriesRepository,
                          AddressesRepository addressesRepository,
                          OrdersRepository ordersRepository,
                          OrderItemsRepository orderItemsRepository,
                          PasswordEncoder passwordEncoder,
                          SeedProperties seed) {
        this.usersRepository = usersRepository;
        this.categoriesRepository = categoriesRepository;
        this.productsRepository = productsRepository;
        this.inventoriesRepository = inventoriesRepository;
        this.addressesRepository = addressesRepository;
        this.ordersRepository = ordersRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.passwordEncoder = passwordEncoder;
        this.seed = seed;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seed.enabled()) {
            log.info("Seed de datos demo desactivado");
            return;
        }

        User admin = createUserIfMissing(seed.adminEmail(), seed.adminPassword(), "Admin", "Demo", Role.ADMIN);
        User customer = createUserIfMissing(seed.customerEmail(), seed.customerPassword(), "Cliente", "Demo", Role.CUSTOMER);

        if (categoriesRepository.count() == 0) {
            createCatalog();
        }

        if (addressesRepository.findByUserIdOrderByIdAsc(customer.getId()).isEmpty()) {
            createAddresses(customer);
        }

        if (ordersRepository.existsByUserId(customer.getId()) == false) {
            createSampleOrder(customer);
        }

        log.info("Seed de datos demo completado");
    }

    private User createUserIfMissing(String email, String password, String name, String lastName, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return usersRepository.findByEmailIgnoreCase(normalizedEmail).map(u -> {
            log.info("El usuario demo {} ya existe; no se modifica", normalizedEmail);
            return u;
        }).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setLastName(lastName);
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setActive(true);
            user.setAddresses(new ArrayList<>());
            User saved = usersRepository.save(user);
            log.info("Usuario demo {} creado con rol {}", normalizedEmail, role);
            return saved;
        });
    }

    private void createCatalog() {
        List<Category> categories = new ArrayList<>();
        categories.add(createCategory("Electrónica", "Dispositivos, accesorios y gadgets"));
        categories.add(createCategory("Ropa", "Prendas de vestir y calzado"));
        categories.add(createCategory("Hogar", "Artículos para el hogar y decoración"));
        categories.add(createCategory("Deportes", "Equipamiento y ropa deportiva"));
        categories.add(createCategory("Libros", "Libros, revistas y material de lectura"));

        categories = categoriesRepository.saveAll(categories);

        List<Product> products = new ArrayList<>();
        products.add(createProduct("Smartphone Samsung Galaxy", BigDecimal.valueOf(699.90), "Último modelo con pantalla AMOLED y 128 GB", categories.get(0), 25, 5));
        products.add(createProduct("Laptop Dell XPS 13", BigDecimal.valueOf(1299.00), "Ultrabook ligera, 16 GB RAM, 512 GB SSD", categories.get(0), 12, 3));
        products.add(createProduct("Auriculares Sony WH-1000XM5", BigDecimal.valueOf(349.50), "Cancelación de ruido inalámbricos", categories.get(0), 40, 8));
        products.add(createProduct("Camiseta algodón orgánico", BigDecimal.valueOf(19.90), "Camiseta unisex 100 % algodón", categories.get(1), 100, 20));
        products.add(createProduct("Zapatillas running", BigDecimal.valueOf(89.00), "Zapatillas para running amortiguadas", categories.get(1), 60, 10));
        products.add(createProduct("Mesa de comedor roble", BigDecimal.valueOf(450.00), "Mesa de madera maciza para 6 personas", categories.get(2), 8, 2));
        products.add(createProduct("Lámpara de pie LED", BigDecimal.valueOf(59.00), "Lámpara de pie regulable, luz cálida", categories.get(2), 30, 5));
        products.add(createProduct("Balón de fútbol", BigDecimal.valueOf(24.90), "Balón oficial talla 5", categories.get(3), 50, 10));
        products.add(createProduct("Curso de Java Spring Boot", BigDecimal.valueOf(39.90), "Libro técnico de programación", categories.get(4), 200, 30));
        products.add(createProduct("Reloj deportivo digital", BigDecimal.valueOf(79.90), "Reloj con GPS y monitor de ritmo cardíaco", categories.get(3), 35, 7));

        productsRepository.saveAll(products);
        log.info("Catálogo demo creado: {} categorías y {} productos", categories.size(), products.size());
    }

    private Category createCategory(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        c.setActive(true);
        return c;
    }

    private Product createProduct(String name, BigDecimal price, String description,
                                  Category category, int stock, int minStock) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCategory(category);
        p.setActive(true);
        p.setImageUrl(null);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setStock(stock);
        inv.setMinStock(minStock);
        p.setInventory(inv);

        return p;
    }

    private void createAddresses(User customer) {
        Address a1 = new Address();
        a1.setUser(customer);
        a1.setStreet("Calle Mayor 12, 3ºA");
        a1.setCity("Madrid");
        a1.setState("Madrid");
        a1.setZipCode("28013");
        a1.setCountry("España");

        Address a2 = new Address();
        a2.setUser(customer);
        a2.setStreet("Avenida de la Constitución 45");
        a2.setCity("Barcelona");
        a2.setState("Barcelona");
        a2.setZipCode("08002");
        a2.setCountry("España");

        addressesRepository.saveAll(List.of(a1, a2));
        log.info("Direcciones demo creadas para el usuario {}", customer.getEmail());
    }

    private void createSampleOrder(User customer) {
        List<Address> addresses = addressesRepository.findByUserIdOrderByIdAsc(customer.getId());
        if (addresses.isEmpty()) {
            log.warn("No se pudo crear el pedido demo: el usuario {} no tiene direcciones", customer.getEmail());
            return;
        }

        Address address = addresses.get(0);

        List<Product> products = productsRepository.findByActiveTrueOrderByNameAsc();
        if (products.isEmpty()) {
            log.warn("No se pudo crear el pedido demo: no hay productos activos");
            return;
        }

        Order order = new Order();
        order.setUser(customer);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Item 1: primer producto, qty 2
        Product p1 = products.get(0);
        OrderItem i1 = new OrderItem();
        i1.setOrder(order);
        i1.setProduct(p1);
        i1.setQuantity(2);
        i1.setUnitPrice(p1.getPrice());
        items.add(i1);
        total = total.add(p1.getPrice().multiply(BigDecimal.valueOf(2)));

        // Item 2: tercer producto, qty 1 (si existe)
        if (products.size() > 2) {
            Product p2 = products.get(2);
            OrderItem i2 = new OrderItem();
            i2.setOrder(order);
            i2.setProduct(p2);
            i2.setQuantity(1);
            i2.setUnitPrice(p2.getPrice());
            items.add(i2);
            total = total.add(p2.getPrice());
        }

        order.setItems(items);
        order.setTotalAmount(total);
        ordersRepository.save(order);
        orderItemsRepository.saveAll(items);
        log.info("Pedido demo creado para el usuario {} con total {}", customer.getEmail(), total);
    }
}
