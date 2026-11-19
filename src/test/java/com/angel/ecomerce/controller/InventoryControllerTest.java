package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.service.inventory.IInventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.category;
import static com.angel.ecomerce.support.TestFixtures.inventory;
import static com.angel.ecomerce.support.TestFixtures.product;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IInventory inventoryService;

    // --- Creación ---

    @Test
    void createReturns201WithProductData() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(inventoryService.save(eq(1L), any(Inventory.class)))
                .willReturn(inventory(10L, product, 10, 2));

        mockMvc.perform(post("/inventories")
                        .param("productId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock":10,"minStock":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("Libro de Java"))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void createWithNegativeStockReturns400() throws Exception {
        mockMvc.perform(post("/inventories")
                        .param("productId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock":-1,"minStock":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stock").value("El stock no puede ser negativo"));
    }

    // --- Consultas ---

    @Test
    void getByProductReturns404WhenMissing() throws Exception {
        given(inventoryService.findByProductId(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/inventories/product/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe inventario para ese producto"));
    }

    @Test
    void lowStockReturnsBoolean() throws Exception {
        given(inventoryService.isLowStock(1L)).willReturn(true);

        mockMvc.perform(get("/inventories/product/1/low-stock"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // --- Movimientos de stock ---

    @Test
    void increaseReturnsUpdatedInventory() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(inventoryService.increaseStock(1L, 5))
                .willReturn(inventory(10L, product, 15, 2));

        mockMvc.perform(patch("/inventories/product/1/increase").param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));
    }

    @Test
    void decreaseWithInsufficientStockReturns409() throws Exception {
        given(inventoryService.decreaseStock(1L, 99))
                .willThrow(new IllegalStateException("Stock insuficiente para el producto con id 1"));

        mockMvc.perform(patch("/inventories/product/1/decrease").param("quantity", "99"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Stock insuficiente para el producto con id 1"));
    }

    // --- Borrado ---

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/inventories/10"))
                .andExpect(status().isNoContent());

        verify(inventoryService).deleteById(10L);
    }
}
