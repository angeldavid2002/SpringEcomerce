package com.angel.ecomerce.integration;

import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.service.storage.IImageStorage;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida de la imagen de una categoría contra el bucket, con la API real.
 *
 * <p>Mismo contrato que los productos, pero con su propio prefijo de claves:
 * la categoría nace, se reemplaza, se quita y se borra llevándose la imagen.</p>
 */
class CategoryImageIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private IImageStorage imageStorage;

    /** Bucket falso: registro de lo subido y lo borrado. */
    private final List<String> uploaded = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void stubStorage() {
        uploaded.clear();
        deleted.clear();
        given(imageStorage.presignedUrl(anyString())).willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key == null ? null : "https://firmada.test/" + key;
        });
        given(imageStorage.store(anyString(), any(ImageUpload.class))).willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            uploaded.add(key);
            return key;
        });
        org.mockito.BDDMockito.willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            deleted.add(key);
            return null;
        }).given(imageStorage).delete(anyString());
    }

    @Test
    void createWithImageUploadsToTheBucketAndReturnsPresignedUrl() throws Exception {
        long categoryId = createCategoryWithImage();
        String url = presignedUrlOf(categoryId);

        assertEquals(1, uploaded.size());
        org.junit.jupiter.api.Assertions.assertTrue(url.startsWith("https://firmada.test/categories/"));
        org.junit.jupiter.api.Assertions.assertTrue(uploaded.get(0).endsWith(".png"));
    }

    @Test
    void editingTheImageDeletesThePreviousOneAndUpdatesTheKey() throws Exception {
        long categoryId = createCategoryWithImage();
        String firstKey = imageKeyOf(categoryId);
        assertEquals(1, uploaded.size());

        performAsAdminMultipart(multipart(HttpMethod.PUT, "/categories/{id}", categoryId)
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes()))
                        .param("name", "Nombre " + categoryId)
                        .param("description", "...")
                        .param("active", "true"))
                .andExpect(status().isOk());

        String secondKey = imageKeyOf(categoryId);

        assertEquals(List.of(firstKey), deleted, "la imagen anterior se borra del bucket");
        assertEquals(2, uploaded.size());
        org.junit.jupiter.api.Assertions.assertNotEquals(firstKey, secondKey,
                "el frontend recibe una clave distinta tras reemplazar la imagen");
    }

    @Test
    void jsonUpdateKeepsTheBucketImageUntouched() throws Exception {
        long categoryId = createCategoryWithImage();

        performAsAdmin(put("/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nombre nuevo","description":"...","active":true}
                                """))
                .andExpect(status().isOk());

        assertEquals(1, uploaded.size());
        org.junit.jupiter.api.Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    void categoryImageCanBeRemovedWithoutDeletingTheCategory() throws Exception {
        long categoryId = createCategoryWithImage();
        String key = imageKeyOf(categoryId);

        performAsAdmin(delete("/categories/{id}/image", categoryId))
                .andExpect(status().isNoContent());
        performAsAdmin(get("/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        assertEquals(List.of(key), deleted);
    }

    @Test
    void deletingTheCategoryDeletesTheImageFromTheBucket() throws Exception {
        long categoryId = createCategoryWithImage();
        String key = imageKeyOf(categoryId);

        performAsAdmin(delete("/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        assertEquals(List.of(key), deleted);
    }

    @Test
    void unsupportedImageTypeReturns400() throws Exception {
        long categoryId = createCategory(suffix());

        performAsAdminMultipart(multipart(HttpMethod.PUT, "/categories/{id}", categoryId)
                        .file(new MockMultipartFile("image", "notas.txt", MediaType.TEXT_PLAIN_VALUE, "hola".getBytes()))
                        .param("name", "Texto")
                        .param("description", "...")
                        .param("active", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Tipo de imagen no permitido")));

        org.junit.jupiter.api.Assertions.assertTrue(uploaded.isEmpty());
    }

    @Test
    void imageEndpointsRequireAdmin() throws Exception {
        long categoryId = createCategoryWithImage();
        String email = "sin-permiso-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");
        String token = tokenFor(email, "CUSTOMER");

        mockMvc.perform(multipart(HttpMethod.PUT, "/categories/{id}", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .file(new MockMultipartFile("image", "x.png", MediaType.IMAGE_PNG_VALUE, "x".getBytes()))
                        .param("name", "Ajeno")
                        .param("description", "...")
                        .param("active", "true"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/categories/{id}/image", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private ResultActions performAsAdminMultipart(
            org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()));
    }

    private long createCategoryWithImage() throws Exception {
        MvcResult result = performAsAdminMultipart(multipart("/categories")
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes()))
                        .param("name", "Categoría " + suffix())
                        .param("description", "Categoría de prueba"))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private String presignedUrlOf(long categoryId) throws Exception {
        MvcResult result = performAsAdmin(get("/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
    }

    private String imageKeyOf(long categoryId) throws Exception {
        MvcResult result = performAsAdmin(get("/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andReturn();
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
        return url.substring("https://firmada.test/".length());
    }
}
