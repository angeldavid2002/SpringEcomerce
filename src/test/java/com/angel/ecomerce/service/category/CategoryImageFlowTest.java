package com.angel.ecomerce.service.category;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.service.storage.IImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Reglas de la imagen de categoría que la prueba HTTP no ve.
 *
 * <p>El recorrido completo (subir, reemplazar, quitar y borrar la categoría)
 * se comprueba en {@code CategoryImageIntegrationTest}; aquí queda solo lo
 * propio del servicio: la clave que guarda y que quitar la imagen llama al
 * bucket.</p>
 */
@ExtendWith(MockitoExtension.class)
class CategoryImageFlowTest {

    @Mock
    private CategoriesRepository categoriesRepository;

    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private IImageStorage imageStorage;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setCategoryPrefix("categories");
        service = new CategoryService(categoriesRepository, productsRepository, imageStorage, properties);
    }

    @Test
    void saveWithImageStoresTheCategoryKey() {
        Category category = new Category();
        category.setName("Libros");
        category.setDescription("Libros y revistas");
        ImageUpload image = imagenPng();
        given(categoriesRepository.save(any(Category.class))).willAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(7L);
            return c;
        });
        given(imageStorage.store(anyString(), any())).willAnswer(invocation -> invocation.getArgument(0));

        Category saved = service.save(category, image);

        assertTrue(saved.getImageKey().startsWith("categories/7/"), "la clave empieza por el prefijo de categorías");
        assertTrue(saved.getImageKey().endsWith(".png"), "la extensión sale del tipo de contenido");
        verify(imageStorage).store(saved.getImageKey(), image);
    }

    @Test
    void removeImageDeletesTheBucketObject() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Libros");
        category.setImageKey("categories/7/queda.png");
        given(categoriesRepository.findById(7L)).willReturn(Optional.of(category));

        service.removeImage(7L);

        assertNull(category.getImageKey(), "la clave se limpia en la base de datos");
        verify(imageStorage).delete("categories/7/queda.png");
        verify(categoriesRepository).save(category);
    }

    private ImageUpload imagenPng() {
        return new ImageUpload("png".getBytes(), "image/png", "portada.png");
    }
}
