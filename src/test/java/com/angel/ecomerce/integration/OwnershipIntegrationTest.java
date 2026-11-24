package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Propiedad por recurso: un CUSTOMER solo ve y modifica sus pedidos y
 * direcciones; un ADMIN pasa siempre. Un recurso ajeno responde 404 para no
 * revelar que el id existe.
 */
class OwnershipIntegrationTest extends IntegrationTestSupport {

    private record Customer(int id, String email, String token) {
    }

    @Test
    void meReturnsOnlyOwnOrders() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        createOrder(ana.id(), createAddress(ana.id()));
        createOrder(luis.id(), createAddress(luis.id()));

        mockMvc.perform(get("/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(ana.id()));
    }

    @Test
    void customerCannotReadAnotherUsersOrder() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long orderId = createOrder(luis.id(), createAddress(luis.id()));

        mockMvc.perform(get("/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El recurso solicitado no existe"));
    }

    @Test
    void adminCanReadAnyOrder() throws Exception {
        Customer luis = newCustomer("luis");
        long orderId = createOrder(luis.id(), createAddress(luis.id()));

        mockMvc.perform(get("/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(luis.id()));
    }

    @Test
    void customerCannotListAnotherUsersOrders() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");

        mockMvc.perform(get("/orders/user/{userId}", luis.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("No tiene permisos para acceder a este recurso"));
    }

    @Test
    void customerCannotCreateOrderForAnotherUser() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long luisAddress = createAddress(luis.id());

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"addressId":%d}
                                """.formatted(luis.id(), luisAddress)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAddItemsToAnotherUsersOrder() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long orderId = createOrder(luis.id(), createAddress(luis.id()));
        long productId = createProduct(createCategory(suffix()), "Producto " + suffix(), "10.00");

        mockMvc.perform(post("/orders/{orderId}/items", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"quantity":1}
                                """.formatted(productId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCanReadOwnOrder() throws Exception {
        Customer ana = newCustomer("ana");
        long orderId = createOrder(ana.id(), createAddress(ana.id()));

        mockMvc.perform(get("/orders/{id}", orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ana.id()));
    }

    @Test
    void meReturnsOnlyOwnAddresses() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        createAddress(ana.id());
        createAddress(luis.id());

        mockMvc.perform(get("/addresses/me").header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(ana.id()));
    }

    @Test
    void customerReadsOwnAddress() throws Exception {
        Customer ana = newCustomer("ana");
        long addressId = createAddress(ana.id());

        mockMvc.perform(get("/addresses/{id}", addressId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) addressId))
                .andExpect(jsonPath("$.userId").value(ana.id()));
    }

    @Test
    void customerCannotReadAnotherUsersAddress() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long addressId = createAddress(luis.id());

        mockMvc.perform(get("/addresses/{id}", addressId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El recurso solicitado no existe"));
    }

    @Test
    void customerCannotUpdateAnotherUsersAddress() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long addressId = createAddress(luis.id());

        mockMvc.perform(put("/addresses/{id}", addressId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"Calle 99","city":"Ciudad","state":"Estado","zipCode":"99999","country":"País"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotDeleteAnotherUsersAddress() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        long addressId = createAddress(luis.id());

        mockMvc.perform(delete("/addresses/{id}", addressId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/addresses/{id}", addressId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotListEveryAddress() throws Exception {
        Customer ana = newCustomer("ana");
        createAddress(ana.id());

        mockMvc.perform(get("/addresses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail")
                        .value("No tiene permisos para acceder a este recurso"));
    }

    @Test
    void adminCanListEveryAddress() throws Exception {
        Customer ana = newCustomer("ana");
        Customer luis = newCustomer("luis");
        createAddress(ana.id());
        createAddress(luis.id());

        mockMvc.perform(get("/addresses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private Customer newCustomer(String prefix) throws Exception {
        String email = prefix + "-" + suffix() + "@example.com";
        MvcResult result = createUserWithRole(email, "CUSTOMER").andReturn();
        return new Customer((int) idFrom(result), email, tokenFor(email, "CUSTOMER"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
