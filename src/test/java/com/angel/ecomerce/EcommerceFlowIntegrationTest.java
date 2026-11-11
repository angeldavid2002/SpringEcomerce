package com.angel.ecomerce;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de aceptación del flujo principal de compra.
 *
 * <p>Recorre el backend como lo haría un cliente real: crea una categoría,
 * un producto, su inventario, un usuario y una dirección; después crea un
 * pedido, añade un item, lo paga, lo cancela y comprueba que el stock se
 * descuenta y se repone. La transacción del test se revierte al final, por
 * lo que no deja datos de negocio en PostgreSQL.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void completePurchaseFlowUpdatesStockAndOrderState() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult categoryResult = mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Aceptación %s","description":"Flujo de compra"}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long categoryId = idFrom(categoryResult);

        MvcResult productResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Producto %s","description":"Producto de prueba","price":19.99,"categoryId":%d}
                                """.formatted(suffix, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value((int) categoryId))
                .andExpect(jsonPath("$.categoryName").value("Aceptación " + suffix))
                .andReturn();
        long productId = idFrom(productResult);

        mockMvc.perform(post("/inventories")
                        .param("productId", String.valueOf(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock":10,"minStock":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.lowStock").value(false));

        String email = "aceptacion-" + suffix + "@example.com";
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cliente","lastName":"Prueba","email":"%s","password":"secreto123","role":"CUSTOMER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long userId = idFrom(userResult);

        MvcResult addressResult = mockMvc.perform(post("/addresses")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"Calle 1","city":"Ciudad","state":"Estado","zipCode":"12345","country":"País"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value((int) userId))
                .andReturn();
        long addressId = idFrom(addressResult);

        MvcResult orderResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"addressId":%d}
                                """.formatted(userId, addressId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isEmpty())
                .andReturn();
        long orderId = idFrom(orderResult);

        MvcResult itemResult = mockMvc.perform(post("/orders/{orderId}/items", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"quantity":2}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.99))
                .andReturn();
        long itemId = ((Number) JsonPath.read(
                itemResult.getResponse().getContentAsString(), "$.items[0].id")).longValue();
        assertEquals(39.98, decimalAt(itemResult, "$.totalAmount"), 0.001);

        MvcResult detailResult = mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value((int) itemId))
                .andExpect(jsonPath("$.items[0].productName").value("Producto " + suffix))
                .andExpect(jsonPath("$.userName").value("Cliente"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        assertEquals(39.98, decimalAt(detailResult, "$.totalAmount"), 0.001);

        MvcResult updatedItemResult = mockMvc.perform(put("/orders/{orderId}/items/{itemId}", orderId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andReturn();
        assertEquals(59.97, decimalAt(updatedItemResult, "$.totalAmount"), 0.001);

        mockMvc.perform(patch("/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        mockMvc.perform(delete("/orders/{orderId}", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private long idFrom(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private double decimalAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).doubleValue();
    }
}
