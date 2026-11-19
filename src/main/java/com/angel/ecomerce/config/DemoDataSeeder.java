package com.angel.ecomerce.config;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Crea dos usuarios de demostración, uno por rol, si todavía no existen.
 *
 * <p>Es idempotente y se puede desactivar con {@code app.seed.enabled=false}.
 * Las contraseñas se hashean y nunca se registran en el log.</p>
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String customerEmail;
    private final String customerPassword;

    public DemoDataSeeder(UsersRepository usersRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed.enabled:true}") boolean enabled,
                          @Value("${app.seed.admin-email:admin@example.com}") String adminEmail,
                          @Value("${app.seed.admin-password:Admin1234!}") String adminPassword,
                          @Value("${app.seed.customer-email:customer@example.com}") String customerEmail,
                          @Value("${app.seed.customer-password:Customer1234!}") String customerPassword) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.customerEmail = customerEmail;
        this.customerPassword = customerPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Seed de usuarios demo desactivado");
            return;
        }
        createIfMissing(adminEmail, adminPassword, "Admin", "Demo", Role.ADMIN);
        createIfMissing(customerEmail, customerPassword, "Cliente", "Demo", Role.CUSTOMER);
    }

    private void createIfMissing(String email, String password, String name, String lastName, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (usersRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            log.info("El usuario demo {} ya existe; no se modifica", normalizedEmail);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setLastName(lastName);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        user.setAddresses(new ArrayList<>());
        usersRepository.save(user);
        log.info("Usuario demo {} creado con rol {}", normalizedEmail, role);
    }
}
