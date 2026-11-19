package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleIntegrationTest extends IntegrationTestSupport {

    @Test
    void completePurchaseFlowUpdatesTotalsAndStock() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");
        createInventory(productId, 10, 2);
        long userId = createUser(suffix);
        long addressId = createAddress(userId);
        long orderId = createOrder(userId, addressId);

        MvcResult itemResult = addItem(orderId, productId, 2).andReturn();
        assertEquals(39.98, decimalAt(itemResult, "$.totalAmount"), 0.001);
        long itemId = itemIdFrom(itemResult);

        performAsAdmin(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Product " + suffix))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(39.98));

        MvcResult updatedItem = updateItemQuantity(orderId, itemId, 3).andReturn();
        assertEquals(59.97, decimalAt(updatedItem, "$.totalAmount"), 0.001);

        patchStatus(orderId, "PAID")
                .andExpect(jsonPath("$.status").value("PAID"));

        performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));

        cancel(orderId)
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        performAsAdmin(delete("/orders/{orderId}", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void addingSameProductIncrementsQuantityAndRemovingItemRecalculatesTotal() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");
        createInventory(productId, 10, 2);
        long userId = createUser(suffix);
        long addressId = createAddress(userId);
        long orderId = createOrder(userId, addressId);

        MvcResult firstAdd = addItem(orderId, productId, 1).andReturn();
        assertEquals(19.99, decimalAt(firstAdd, "$.totalAmount"), 0.001);

        MvcResult secondAdd = addItem(orderId, productId, 1).andReturn();
        assertEquals(2, intAt(secondAdd, "$.items[0].quantity"));
        assertEquals(39.98, decimalAt(secondAdd, "$.totalAmount"), 0.001);

        long itemId = itemIdFrom(secondAdd);
        performAsAdmin(delete("/orders/{orderId}/items/{itemId}", orderId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0.00));

        MvcResult thirdAdd = addItem(orderId, productId, 1).andReturn();
        long newItemId = itemIdFrom(thirdAdd);

        updateItemQuantity(orderId, newItemId, 0)
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0.00));
    }

    @Test
    void paymentFailsWhenStockIsInsufficient() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");
        createInventory(productId, 1, 0);
        long userId = createUser(suffix);
        long addressId = createAddress(userId);
        long orderId = createOrder(userId, addressId);

        addItem(orderId, productId, 2);

        performAsAdmin(patch("/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        performAsAdmin(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void invalidTransitionFromPendingToDeliveredReturns400() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");
        createInventory(productId, 10, 2);
        long userId = createUser(suffix);
        long addressId = createAddress(userId);
        long orderId = createOrder(userId, addressId);

        addItem(orderId, productId, 1);

        performAsAdmin(patch("/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        performAsAdmin(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void cancelFromPendingDoesNotMoveStockAndShippedOrderCannotBeCancelled() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");
        createInventory(productId, 10, 2);
        long userId = createUser(suffix);
        long addressId = createAddress(userId);

        long pendingOrderId = createOrder(userId, addressId);
        addItem(pendingOrderId, productId, 2);
        cancel(pendingOrderId);

        performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        long shippedOrderId = createOrder(userId, addressId);
        addItem(shippedOrderId, productId, 2);
        patchStatus(shippedOrderId, "PAID");
        patchStatus(shippedOrderId, "SHIPPED");

        performAsAdmin(patch("/orders/{orderId}/cancel", shippedOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));
    }

    @Test
    void orderRequiresAddressOwnedByTheSameUser() throws Exception {
        String suffix = suffix();
        long ownerId = createUser("owner-" + suffix);
        long otherId = createUser("other-" + suffix);
        long addressId = createAddress(ownerId);

        performAsAdmin(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"addressId":%d}
                                """.formatted(otherId, addressId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
