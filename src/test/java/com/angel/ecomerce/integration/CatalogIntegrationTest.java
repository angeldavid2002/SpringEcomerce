package com.angel.ecomerce.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        performAsAdmin(get("/inventories/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStock").value(true));

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

        mockMvc.perform(get("/products/search").param("name", productName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) productId));

        performAsAdmin(patch("/products/{id}/deactivate", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/products/search").param("name", productName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void publicListingHidesInactiveCatalogButAdminSeesIt() throws Exception {
        String suffix = suffix();
        String categoryName = "Category " + suffix;
        String productName = "Hidden " + suffix;
        long categoryId = createCategoryNamed(categoryName);
        long productId = createProduct(categoryId, productName, "19.99");

        performAsAdmin(patch("/products/{id}/deactivate", productId))
                .andExpect(status().isOk());
        performAsAdmin(put("/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","active":false}
                                """.formatted(categoryName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/products/category/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(productId)).isEmpty());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(categoryId)).isEmpty());

        performAsAdmin(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(productId)).isNotEmpty());

        performAsAdmin(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(categoryId)).isNotEmpty());
    }

    @Test
    void publicPriceAndRatingFiltersHideInactiveProducts() throws Exception {
        String suffix = suffix();
        long categoryId = createCategoryNamed("PriceFilter " + suffix);
        long inactiveId = createProduct(categoryId, "Inactive " + suffix, "15.00");

        performAsAdmin(patch("/products/{id}/deactivate", inactiveId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products").param("minPrice", "10").param("maxPrice", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(inactiveId)).isEmpty());

        mockMvc.perform(get("/products").param("minRating", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(inactiveId)).isEmpty());

        // El admin, que ve también los inactivos, sí los obtiene.
        performAsAdmin(get("/products").param("minPrice", "10").param("maxPrice", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(inactiveId)).isNotEmpty());

        performAsAdmin(get("/products").param("minRating", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(inactiveId)).isNotEmpty());
    }

    @Test
    void priceFiltersStillReturnActiveProductsInRange() throws Exception {
        String suffix = suffix();
        long categoryId = createCategoryNamed("Range " + suffix);
        long productId = createProduct(categoryId, "Range " + suffix, "20.00");

        mockMvc.perform(get("/products").param("minPrice", "10").param("maxPrice", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(productId)).isNotEmpty());

        mockMvc.perform(get("/products").param("minPrice", "40").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("minPrice no puede ser mayor que maxPrice"));
    }

    @Test
    void publicCatalogExposesStockWithoutAuthentication() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Visible " + suffix(), "19.99");
        createInventory(productId, 10, 2);

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.available").value(true));
    }
}
