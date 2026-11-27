package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.category.ICategory;
import com.angel.ecomerce.service.storage.IImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.category;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICategory categoryService;

    @MockitoBean
    private OwnershipGuard ownershipGuard;

    @MockitoBean
    private IImageStorage imageStorage;

    @BeforeEach
    void stubStorage() {
        given(imageStorage.presignedUrl(anyString()))
                .willAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    return key == null ? null : "https://firmada.example/" + key;
                });
    }

    // --- Creación ---

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
    void createWithImageUploadsToTheBucketAndReturnsPresignedUrl() throws Exception {
        Category category = category(1L, "Libros");
        category.setImageKey("categories/1/clave.png");
        given(categoryService.save(any(Category.class), any())).willReturn(category);

        mockMvc.perform(multipart("/categories")
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "binarios".getBytes()))
                        .param("name", "Libros")
                        .param("description", "Libros y revistas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://firmada.example/categories/1/clave.png"));
    }

    @Test
    void updateWithImageReturnsPresignedUrl() throws Exception {
        Category category = category(1L, "Libros");
        category.setImageKey("categories/1/clave.png");
        given(categoryService.updateById(any(Long.class), any(Category.class), any())).willReturn(category);

        mockMvc.perform(multipart(HttpMethod.PUT, "/categories/{id}", 1L)
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "binarios".getBytes()))
                        .param("name", "Libros")
                        .param("description", "Libros y revistas")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://firmada.example/categories/1/clave.png"));
    }

    // --- Consultas ---

    @Test
    void getAllReturnsOnlyActiveCategoriesForPublic() throws Exception {
        given(categoryService.getActive()).willReturn(List.of(
                category(1L, "Libros"),
                category(2L, "Música")
        ));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].name").value("Música"));
    }

    @Test
    void getAllReturnsEveryCategoryForAdmin() throws Exception {
        given(ownershipGuard.isAdmin()).willReturn(true);
        given(categoryService.getAll()).willReturn(List.of(
                category(1L, "Libros"),
                category(2L, "Música")
        ));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(categoryService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe una categoría con ese id"));
    }

    // --- Borrado ---

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteById(1L);
    }

    @Test
    void deleteImageReturns204() throws Exception {
        mockMvc.perform(delete("/categories/1/image"))
                .andExpect(status().isNoContent());

        verify(categoryService).removeImage(1L);
    }
}
