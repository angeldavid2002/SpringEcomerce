package com.angel.ecomerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del seed de usuarios demo.
 *
 * <p>Las credenciales solo se validan cuando el seed está activo, de modo que
 * en tests y en producción se puede desactivar sin definirlas.</p>
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled,
                             String adminEmail,
                             String adminPassword,
                             String customerEmail,
                             String customerPassword) {

    public SeedProperties {
        if (enabled) {
            require(adminEmail, "app.seed.admin-email (SEED_ADMIN_EMAIL)");
            require(adminPassword, "app.seed.admin-password (SEED_ADMIN_PASSWORD)");
            require(customerEmail, "app.seed.customer-email (SEED_CUSTOMER_EMAIL)");
            require(customerPassword, "app.seed.customer-password (SEED_CUSTOMER_PASSWORD)");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " es obligatorio cuando app.seed.enabled=true");
        }
    }
}
