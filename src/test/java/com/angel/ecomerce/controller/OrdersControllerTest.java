package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.service.order.IOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.angel.ecomerce.support.TestFixtures.address;
import static com.angel.ecomerce.support.TestFixtures.category;
import static com.angel.ecomerce.support.TestFixtures.item;
import static com.angel.ecomerce.support.TestFixtures.order;
import static com.angel.ecomerce.support.TestFixtures.product;
import static com.angel.ecomerce.support.TestFixtures.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdersController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOrder orderService;

    // --- Creación y consultas ---

    @Test
    void createReturns201PendingOrder() throws Exception {
        User user = user(1L, "ana@example.com");
        Address address = address(1L, user);
        given(orderService.create(1L, 1L))
                .willReturn(order(1L, user, address, OrderStatus.PENDING, "0.00"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"addressId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(0.00))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void getByStatusWithInvalidValueReturns400() throws Exception {
        mockMvc.perform(get("/orders/status/paid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    // --- Items ---

    @Test
    void addItemReturnsUpdatedOrder() throws Exception {
        Order order = orderWithItem(2, "39.98");
        given(orderService.addItem(1L, 1L, 2)).willReturn(order);

        mockMvc.perform(post("/orders/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.99))
                .andExpect(jsonPath("$.totalAmount").value(39.98));
    }

    @Test
    void updateItemQuantityReturnsUpdatedOrder() throws Exception {
        Order order = orderWithItem(3, "59.97");
        given(orderService.updateItemQuantity(1L, 10L, 3)).willReturn(order);

        mockMvc.perform(put("/orders/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(59.97));
    }

    // --- Estados ---

    @Test
    void updateStatusReturnsPaidOrder() throws Exception {
        User user = user(1L, "ana@example.com");
        Address address = address(1L, user);
        given(orderService.updateStatus(1L, OrderStatus.PAID))
                .willReturn(order(1L, user, address, OrderStatus.PAID, "39.98"));

        mockMvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PAID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        User user = user(1L, "ana@example.com");
        Address address = address(1L, user);
        given(orderService.cancel(1L))
                .willReturn(order(1L, user, address, OrderStatus.CANCELLED, "39.98"));

        mockMvc.perform(patch("/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deleteReturns409WhenOrderIsNotPending() throws Exception {
        doThrow(new IllegalStateException("Solo se pueden eliminar pedidos en estado PENDING"))
                .when(orderService).deleteById(1L);

        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Solo se pueden eliminar pedidos en estado PENDING"));
    }

    private Order orderWithItem(int quantity, String total) {
        User user = user(1L, "ana@example.com");
        Address address = address(1L, user);
        Category category = category(1L, "Libros");
        Product product = product(1L, "Libro de Java", "19.99", category);
        Order order = order(1L, user, address, OrderStatus.PENDING, total);
        item(10L, order, product, quantity, "19.99");
        return order;
    }
}
