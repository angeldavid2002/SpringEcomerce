package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.Order;
import com.angel.ecomerce.entity.OrderItem;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.OrderStatus;
import com.angel.ecomerce.service.orderitem.IOrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.address;
import static com.angel.ecomerce.support.TestFixtures.category;
import static com.angel.ecomerce.support.TestFixtures.item;
import static com.angel.ecomerce.support.TestFixtures.order;
import static com.angel.ecomerce.support.TestFixtures.product;
import static com.angel.ecomerce.support.TestFixtures.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOrderItem orderItemService;

    @Test
    void getByOrderReturnsItems() throws Exception {
        OrderItem item = orderItem(2, "19.99");
        given(orderItemService.getByOrderId(1L)).willReturn(List.of(item));

        mockMvc.perform(get("/order-items").param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].productName").value("Libro de Java"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].subtotal").value(39.98));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(orderItemService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/order-items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un item de pedido con ese id"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/order-items/10"))
                .andExpect(status().isNoContent());

        verify(orderItemService).deleteById(10L);
    }

    private OrderItem orderItem(int quantity, String unitPrice) {
        User user = user(1L, "ana@example.com");
        Address address = address(1L, user);
        Product product = product(1L, "Libro de Java", unitPrice, category(1L, "Libros"));
        Order order = order(1L, user, address, OrderStatus.PENDING, "39.98");
        return item(10L, order, product, quantity, unitPrice);
    }
}
