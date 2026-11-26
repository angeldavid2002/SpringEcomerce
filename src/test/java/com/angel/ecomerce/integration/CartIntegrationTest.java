package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Carrito sincronizado en backend: cada cliente opera sobre su propio carrito.
 *
 * <p>El caso {@link #getCartReturnsItemsWithProductData()} cubre la regresión
 * del 500 por carga perezosa del producto al serializar el carrito.</p>
 */
class CartIntegrationTest extends IntegrationTestSupport {

    @Test
    void emptyCartIsCreatedAndReturned() throws Exception {
        String token = customerToken("cart-empty");

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getCartReturnsItemsWithProductData() throws Exception {
        String token = customerToken("cart-items");
        String name = "Camiseta " + suffix();
        long productId = createProduct(createCategory(suffix()), name, "10.00");

        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value((int) productId))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(20.00));

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value((int) productId))
                .andExpect(jsonPath("$.items[0].productName").value(name))
                .andExpect(jsonPath("$.items[0].productPrice").value(10.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(20.00))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void addingSameProductTwiceIncrementsQuantity() throws Exception {
        String token = customerToken("cart-increment");
        long productId = newProduct("Teclado", "25.00");

        addItem(token, productId, 1);
        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(75.00));
    }

    @Test
    void updateQuantityReplacesValue() throws Exception {
        String token = customerToken("cart-update");
        long productId = newProduct("Mouse", "15.00");
        addItem(token, productId, 2);

        mockMvc.perform(put("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.subtotal").value(75.00));

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void updateMissingItemReturns404() throws Exception {
        String token = customerToken("cart-update-missing");
        long productId = newProduct("Monitor", "100.00");

        mockMvc.perform(put("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El producto no está en el carrito"));
    }

    @Test
    void removeItemDeletesOnlyThatLine() throws Exception {
        String token = customerToken("cart-remove");
        long first = newProduct("Auriculares", "30.00");
        long second = newProduct("Cable", "5.00");
        addItem(token, first, 1);
        addItem(token, second, 1);

        mockMvc.perform(delete("/cart/{productId}", first)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value((int) second));
    }

    @Test
    void removingMissingItemIsIdempotent() throws Exception {
        String token = customerToken("cart-remove-missing");
        long productId = newProduct("Funda", "8.00");

        mockMvc.perform(delete("/cart/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearCartRemovesEveryItem() throws Exception {
        String token = customerToken("cart-clear");
        addItem(token, newProduct("Bolsa", "12.00"), 1);
        addItem(token, newProduct("Gorra", "9.00"), 2);

        mockMvc.perform(delete("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void quantityBelowOneReturns400WithFieldError() throws Exception {
        String token = customerToken("cart-invalid");
        long productId = newProduct("Reloj", "60.00");

        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").value("La cantidad debe ser al menos 1"));
    }

    @Test
    void missingProductReturns404() throws Exception {
        String token = customerToken("cart-missing-product");

        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(999_999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un producto con ese id"));
    }

    @Test
    void inactiveProductReturns409() throws Exception {
        String token = customerToken("cart-inactive");
        long productId = newProduct("Producto descatalogado", "5.00");

        performAsAdmin(patch("/products/{id}/deactivate", productId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El producto no está disponible"));
    }

    @Test
    void cartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(1L, 1)))
                .andExpect(status().isUnauthorized());
    }

    private String customerToken(String prefix) throws Exception {
        String email = prefix + "-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");
        return tokenFor(email, "CUSTOMER");
    }

    private long newProduct(String name, String price) throws Exception {
        String uniqueName = name + " " + suffix();
        return createProduct(createCategory(suffix()), uniqueName, price);
    }

    private void addItem(String token, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, quantity)))
                .andExpect(status().isOk());
    }

    private String itemJson(long productId, int quantity) {
        return """
                {"productId":%d,"quantity":%d}
                """.formatted(productId, quantity);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
