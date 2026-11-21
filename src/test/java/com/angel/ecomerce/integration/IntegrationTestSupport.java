package com.angel.ecomerce.integration;

import com.angel.ecomerce.security.JwtProperties;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base común para las pruebas de integración.
 *
 * <p>Centraliza la creación de datos por HTTP y evita repetir el mismo flujo
 * en cada escenario. Cada test corre dentro de una transacción que se revierte.</p>
 */
@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "app.jwt.secret=test-secret-with-at-least-32-bytes!!"
})
@AutoConfigureMockMvc
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private String adminToken;

    /**
     * Ejecuta la petición con un token de ADMIN. Los escenarios de integración
     * prueban reglas de negocio, no permisos; la autorización se cubre en
     * SecurityIntegrationTest.
     */
    protected ResultActions performAsAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()));
    }

    protected String adminToken() {
        if (adminToken == null) {
            adminToken = tokenFor("admin@test.local", "ADMIN");
        }
        return adminToken;
    }

    protected String tokenFor(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiration()))
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(jwtProperties.algorithm()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    protected String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected long createCategory(String suffix) throws Exception {
        return createCategoryNamed("Category " + suffix);
    }

    protected long createCategoryNamed(String name) throws Exception {
        MvcResult result = performAsAdmin(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Categoría de prueba"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    protected long createProduct(long categoryId, String name, String price) throws Exception {
        MvcResult result = performAsAdmin(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Producto de prueba","price":%s,"categoryId":%d}
                                """.formatted(name, price, categoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    protected ResultActions createInventory(long productId, int stock, int minStock) throws Exception {
        return performAsAdmin(post("/inventories")
                        .param("productId", String.valueOf(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock":%d,"minStock":%d}
                                """.formatted(stock, minStock)))
                .andExpect(status().isCreated());
    }

    protected long createUser(String suffix) throws Exception {
        return idFrom(createUserWithRole("user-" + suffix + "@example.com", "CUSTOMER").andReturn());
    }

    protected ResultActions createUserWithRole(String email, String role) throws Exception {
        String rolePart = role == null ? "" : ", \"role\":\"%s\"".formatted(role);
        return performAsAdmin(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cliente","lastName":"Prueba","email":"%s","password":"secreto123"%s}
                                """.formatted(email, rolePart)))
                .andExpect(status().isCreated());
    }

    protected long createAddress(long userId) throws Exception {
        MvcResult result = performAsAdmin(post("/addresses")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"Calle 1","city":"Ciudad","state":"Estado","zipCode":"12345","country":"País"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    protected long createOrder(long userId, long addressId) throws Exception {
        MvcResult result = performAsAdmin(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"addressId":%d}
                                """.formatted(userId, addressId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    protected ResultActions addItem(long orderId, long productId, int quantity) throws Exception {
        return performAsAdmin(post("/orders/{orderId}/items", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"quantity":%d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isOk());
    }

    protected ResultActions updateItemQuantity(long orderId, long itemId, int quantity) throws Exception {
        return performAsAdmin(put("/orders/{orderId}/items/{itemId}", orderId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":%d}
                                """.formatted(quantity)))
                .andExpect(status().isOk());
    }

    protected ResultActions patchStatus(long orderId, String status) throws Exception {
        return performAsAdmin(patch("/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(status)))
                .andExpect(status().isOk());
    }

    protected ResultActions cancel(long orderId) throws Exception {
        return performAsAdmin(patch("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk());
    }

    protected long idFrom(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    protected long itemIdFrom(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.items[0].id");
        return id.longValue();
    }

    protected double decimalAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).doubleValue();
    }

    protected int intAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).intValue();
    }
}
