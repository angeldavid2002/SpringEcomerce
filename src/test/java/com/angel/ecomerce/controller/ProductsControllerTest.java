package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.service.products.IProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductsController.class)
class ProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProduct productsService;

    @Test
    void searchReturnsMappedActiveProducts() throws Exception {
        Product product = product(1L, "Libro de Java", true);
        given(productsService.searchActiveByName("libro")).willReturn(List.of(product));

        mockMvc.perform(get("/products/search").param("name", "libro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Libro de Java"))
                .andExpect(jsonPath("$[0].categoryName").value("Libros"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void createReturns201WithCategoryData() throws Exception {
        Product product = product(1L, "Libro de Java", true);
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

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(productsService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un producto con ese id"));
    }

    @Test
    void deactivateReturnsUpdatedProduct() throws Exception {
        Product product = product(1L, "Libro de Java", false);
        given(productsService.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(patch("/products/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(productsService).deactivateById(1L);
    }

    private Product product(Long id, String name, boolean active) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Libros");
        category.setActive(true);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Descripción");
        product.setPrice(new BigDecimal("19.99"));
        product.setActive(active);
        product.setCategory(category);
        product.setCreated(LocalDateTime.now());
        product.setUpdated(LocalDateTime.now());
        return product;
    }
}
