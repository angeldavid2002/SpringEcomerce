package com.angel.ecomerce.service.products;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.service.storage.IImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Reglas de la imagen de producto que la prueba HTTP no ve.
 *
 * <p>El recorrido completo (subir, reemplazar, borrar, 400 por tipo o tamaño)
 * se comprueba en {@code ProductImageIntegrationTest}; aquí queda solo lo
 * propio del servicio: la clave que guarda y que desactivar no toca el bucket.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductImageFlowTest {

    @Mock
    private ProductsRepository productsRepository;
    @Mock
    private CategoriesRepository categoriesRepository;
    @Mock
    private IImageStorage imageStorage;

    private ProductsService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setPrefix("products");
        service = new ProductsService(productsRepository, categoriesRepository, null,
                imageStorage, properties);
    }

    @Test
    void saveWithImageStoresKeyAndClearsLegacyUrl() {
        Product product = productoNuevo("https://externa.example/imagen.jpg");
        ImageUpload image = imagenPng();
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(categoria()));
        given(imageStorage.store(anyString(), any())).willAnswer(invocation -> invocation.getArgument(0));

        Product saved = service.save(product, image);

        assertTrue(saved.getImageKey().startsWith("products/10/"), "la clave empieza por el prefijo y el id");
        assertTrue(saved.getImageKey().endsWith(".png"), "la extensión sale del tipo de contenido");
        assertNull(saved.getImageUrl(), "la imagen del bucket sustituye a la URL externa");
        verify(imageStorage).store(saved.getImageKey(), image);
    }

    @Test
    void deactivateKeepsTheImage() {
        Product existente = new Product();
        existente.setId(13L);
        existente.setName("Libro");
        existente.setPrice(new BigDecimal("9.99"));
        existente.setActive(true);
        existente.setCategory(categoria());
        existente.setImageKey("products/13/queda.jpg");

        given(productsRepository.findById(13L)).willReturn(Optional.of(existente));

        service.deactivateById(13L);

        assertEquals("products/13/queda.jpg", existente.getImageKey());
        verify(imageStorage, never()).delete(anyString());
    }

    private Product productoNuevo(String imageUrl) {
        Product product = new Product();
        product.setName("Libro de Java");
        product.setDescription("Libro");
        product.setPrice(new BigDecimal("19.99"));
        product.setImageUrl(imageUrl);
        product.setCategory(categoria());
        return product;
    }

    private Category categoria() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Libros");
        category.setDescription("Categoría de prueba");
        return category;
    }

    private ImageUpload imagenPng() {
        return new ImageUpload("png".getBytes(), "image/png", "portada.png");
    }
}