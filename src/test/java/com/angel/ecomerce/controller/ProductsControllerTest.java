package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.service.products.IProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.category;
import static com.angel.ecomerce.support.TestFixtures.inventory;
import static com.angel.ecomerce.support.TestFixtures.product;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductsController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProduct productsService;

    // --- Consultas ---

    @Test
    void searchReturnsMappedActiveProducts() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(productsService.searchActiveByName("libro")).willReturn(List.of(product));

        mockMvc.perform(get("/products/search").param("name", "libro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Libro de Java"))
                .andExpect(jsonPath("$[0].categoryName").value("Libros"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getByCategoryReturnsMappedProducts() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(productsService.getByCategory(1L)).willReturn(List.of(product));

        mockMvc.perform(get("/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Libros"));
    }

    @Test
    void getByIdIncludesStockAndAvailability() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        product.setInventory(inventory(1L, product, 7, 2));
        given(productsService.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getByIdWithoutInventoryReturnsNullStock() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(productsService.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(nullValue()))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(productsService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un producto con ese id"));
    }

    // --- Creación ---

    @Test
    void createReturns201WithCategoryData() throws Exception {
        Product product = product(1L, "Libro de Java", "19.99", category(1L, "Libros"));
        given(productsService.save(any(Product.class))).willReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Libro de Java","description":"Libro","price":19.99,"categoryId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Libros"));
    }

    @Test
    void createWithNegativePriceReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Libro de Java","price":-1,"categoryId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("El precio no puede ser negativo"));
    }

    // --- Desactivación y borrado ---

    @Test
    void deactivateReturnsUpdatedProduct() throws Exception {
        Category category = category(1L, "Libros");
        Product product = product(1L, "Libro de Java", "19.99", category, false);
        given(productsService.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(patch("/products/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(productsService).deactivateById(1L);
    }

    @Test
    void deleteReturns409WhenProductHasOrderItems() throws Exception {
        doThrow(new IllegalStateException(
                "No se puede eliminar el producto porque está asociado a pedidos; desactívelo en su lugar"))
                .when(productsService).deleteById(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
