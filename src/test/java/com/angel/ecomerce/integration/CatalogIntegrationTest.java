package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends IntegrationTestSupport {

    @Test
    void categoryNameMustBeUnique() throws Exception {
        String suffix = suffix();
        String name = "Category " + suffix;
        createCategoryNamed(name);

        performAsAdmin(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ya existe una categoría con ese nombre"));
    }

    @Test
    void productRequiresAnExistingCategory() throws Exception {
        performAsAdmin(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Producto sin categoría","price":19.99,"categoryId":999999999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void inventoryTracksLowStockAndRejectsOverdraft() throws Exception {
        String suffix = suffix();
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, "Product " + suffix, "19.99");

        createInventory(productId, 2, 2)
                .andExpect(jsonPath("$.stock").value(2))
                .andExpect(jsonPath("$.lowStock").value(true));

        performAsAdmin(get("/inventories/product/{productId}/low-stock", productId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        performAsAdmin(patch("/inventories/product/{productId}/decrease", productId)
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(1))
                .andExpect(jsonPath("$.lowStock").value(true));

        performAsAdmin(patch("/inventories/product/{productId}/increase", productId)
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.lowStock").value(false));

        performAsAdmin(patch("/inventories/product/{productId}/decrease", productId)
                        .param("quantity", "99"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void searchReturnsOnlyActiveProducts() throws Exception {
        String suffix = suffix();
        String productName = "Product " + suffix;
        long categoryId = createCategory(suffix);
        long productId = createProduct(categoryId, productName, "19.99");

        performAsAdmin(get("/products/search").param("name", productName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) productId));

        performAsAdmin(patch("/products/{id}/deactivate", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        performAsAdmin(get("/products/search").param("name", productName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
