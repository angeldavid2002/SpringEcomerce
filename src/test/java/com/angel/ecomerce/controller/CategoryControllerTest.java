package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.service.category.ICategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICategory categoryService;

    @Test
    void createReturns201AndBody() throws Exception {
        Category category = category(1L, "Libros");
        given(categoryService.save(any(Category.class))).willReturn(category);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Libros","description":"Libros y revistas"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Libros"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createWithBlankNameReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("El nombre es obligatorio"));
    }

    @Test
    void getAllReturnsMappedList() throws Exception {
        given(categoryService.getAll()).willReturn(List.of(category(1L, "Libros"), category(2L, "Música")));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Música"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(categoryService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe una categoría con ese id"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteById(1L);
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Descripción");
        category.setActive(true);
        category.setCreated(LocalDateTime.now());
        category.setUpdated(LocalDateTime.now());
        return category;
    }
}
