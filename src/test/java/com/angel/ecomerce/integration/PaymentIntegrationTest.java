package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pago del pedido por parte del dueño: descuenta stock, respeta la propiedad
 * del recurso y deja el cambio de estado general en manos de ADMIN.
 */
class PaymentIntegrationTest extends IntegrationTestSupport {

    @Test
    void ownerPaysOwnOrderAndStockIsDeducted() throws Exception {
        String email = "paga-" + suffix() + "@example.com";
        long userId = idFrom(createUserWithRole(email, "CUSTOMER").andReturn());
        long addressId = createAddress(userId);

        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Producto " + suffix(), "19.99");
        createInventory(productId, 10, 2);

        long orderId = createOrder(userId, addressId);
        addItem(orderId, productId, 2);

        String token = tokenFor(email, "CUSTOMER");
        mockMvc.perform(post("/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        MvcResult inventory = performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(intAt(inventory, "$.stock")).isEqualTo(8);
    }

    @Test
    void customerCannotPayAnotherUsersOrder() throws Exception {
        String ownerEmail = "dueno-" + suffix() + "@example.com";
        long ownerId = idFrom(createUserWithRole(ownerEmail, "CUSTOMER").andReturn());
        long addressId = createAddress(ownerId);
        long orderId = createOrder(ownerId, addressId);

        String otherEmail = "otro-" + suffix() + "@example.com";
        createUserWithRole(otherEmail, "CUSTOMER");

        String token = tokenFor(otherEmail, "CUSTOMER");
        mockMvc.perform(post("/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void payFailsWhenStockIsInsufficient() throws Exception {
        String email = "sin-stock-" + suffix() + "@example.com";
        long userId = idFrom(createUserWithRole(email, "CUSTOMER").andReturn());
        long addressId = createAddress(userId);

        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Escaso " + suffix(), "19.99");
        createInventory(productId, 0, 0);

        long orderId = createOrder(userId, addressId);
        addItem(orderId, productId, 1);

        String token = tokenFor(email, "CUSTOMER");
        mockMvc.perform(post("/orders/{orderId}/pay", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanPayAnyOrder() throws Exception {
        long userId = createUser(suffix());
        long addressId = createAddress(userId);

        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Producto " + suffix(), "19.99");
        createInventory(productId, 5, 1);

        long orderId = createOrder(userId, addressId);
        addItem(orderId, productId, 1);

        performAsAdmin(post("/orders/{orderId}/pay", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
