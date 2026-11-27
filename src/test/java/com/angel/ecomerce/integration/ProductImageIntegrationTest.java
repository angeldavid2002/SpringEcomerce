package com.angel.ecomerce.integration;

import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.service.storage.IImageStorage;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida de la imagen de un producto contra el bucket, con la API real.
 *
 * <p>El bucket se sustituye por uno en memoria que registra lo subido y lo
 * borrado: asÃ­ se comprueba el contrato (reemplazar borrando la anterior y
 * llevarse la imagen al borrar el producto) sin depender de B2.</p>
 */
class ProductImageIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private IImageStorage imageStorage;

    /** Bucket falso: clave -> contenido. */
    private final List<String> deleted = new ArrayList<>();
    private final List<String> uploaded = new ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void stubStorage() {
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
        long categoryId = createCategory(suffix());
        String imageUrl = createProductWithImage(categoryId);

        org.junit.jupiter.api.Assertions.assertEquals(1, uploaded.size());
        org.junit.jupiter.api.Assertions.assertTrue(imageUrl.startsWith("https://firmada.test/products/"));
        org.junit.jupiter.api.Assertions.assertTrue(uploaded.get(0).endsWith(".png"));
    }

    @Test
    void editingTheImageDeletesThePreviousOneAndUpdatesTheKey() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Con imagen " + suffix(), "10.00");
        String firstKey = uploadImage(productId, categoryId);
        String firstUrl = presignedUrlOf(productId);
        assertEquals(1, uploaded.size());

        String secondKey = uploadImage(productId, categoryId);
        String secondUrl = presignedUrlOf(productId);

        assertEquals(List.of(firstKey), deleted, "la imagen anterior se borra del bucket");
        assertEquals(2, uploaded.size());
        org.junit.jupiter.api.Assertions.assertNotEquals(firstUrl, secondUrl,
                "el frontend recibe una URL distinta tras reemplazar la imagen");
    }

    @Test
    void jsonUpdateKeepsTheBucketImageUntouched() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Sin tocar " + suffix(), "10.00");
        uploadImage(productId, categoryId);

        performAsAdmin(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nombre nuevo","description":"...","price":12.00,"categoryId":%d,"active":true}
                                """.formatted(categoryId)))
                .andExpect(status().isOk());

        assertEquals(1, uploaded.size());
        org.junit.jupiter.api.Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    void productImageCanBeRemovedWithoutDeletingTheProduct() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Quitar imagen " + suffix(), "10.00");
        String key = uploadImage(productId, categoryId);

        performAsAdmin(delete("/products/{id}/image", productId))
                .andExpect(status().isNoContent());
        performAsAdmin(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        assertEquals(List.of(key), deleted);
    }

    @Test
    void deletingTheProductDeletesTheImageFromTheBucket() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Borrar todo " + suffix(), "10.00");
        String key = uploadImage(productId, categoryId);

        performAsAdmin(delete("/products/{id}", productId))
                .andExpect(status().isNoContent());

        assertEquals(List.of(key), deleted);
    }

    @Test
    void oversizedImageReturns400AndDoesNotTouchTheBucket() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Grande " + suffix(), "10.00");

        byte[] huge = new byte[6 * 1024 * 1024];
        performAsAdminMultipart(multipart(HttpMethod.PUT, "/products/{id}", productId)
                        .file(new MockMultipartFile("image", "grande.png", MediaType.IMAGE_PNG_VALUE, huge))
                        .param("name", "Grande")
                        .param("description", "...")
                        .param("price", "10.00")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("active", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("tamaño máximo")));

        org.junit.jupiter.api.Assertions.assertTrue(uploaded.isEmpty());
    }

    @Test
    void unsupportedImageTypeReturns400() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Texto " + suffix(), "10.00");

        performAsAdminMultipart(multipart(HttpMethod.PUT, "/products/{id}", productId)
                        .file(new MockMultipartFile("image", "notas.txt", MediaType.TEXT_PLAIN_VALUE, "hola".getBytes()))
                        .param("name", "Texto")
                        .param("description", "...")
                        .param("price", "10.00")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("active", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Tipo de imagen no permitido")));
    }

    @Test
    void imageEndpointsRequireAdmin() throws Exception {
        long categoryId = createCategory(suffix());
        long productId = createProduct(categoryId, "Ajeno " + suffix(), "10.00");
        String email = "sin-permiso-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");
        String token = tokenFor(email, "CUSTOMER");

        mockMvc.perform(multipart(HttpMethod.PUT, "/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .file(new MockMultipartFile("image", "x.png", MediaType.IMAGE_PNG_VALUE, "x".getBytes()))
                        .param("name", "Ajeno")
                        .param("description", "...")
                        .param("price", "10.00")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("active", "true"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/products/{id}/image", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions performAsAdminMultipart(
            org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()));
    }

    private String createProductWithImage(long categoryId) throws Exception {
        MvcResult result = performAsAdminMultipart(multipart("/products")
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes()))
                        .param("name", "Con imagen " + suffix())
                        .param("description", "...")
                        .param("price", "10.00")
                        .param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
    }

    private String uploadImage(long productId, long categoryId) throws Exception {
        MvcResult result = performAsAdminMultipart(multipart(HttpMethod.PUT, "/products/{id}", productId)
                        .file(new MockMultipartFile("image", "portada.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes()))
                        .param("name", "Nombre")
                        .param("description", "...")
                        .param("price", "10.00")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andReturn();
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
        return url.substring("https://firmada.test/".length());
    }

    private String presignedUrlOf(long productId) throws Exception {
        MvcResult result = performAsAdmin(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
    }
}