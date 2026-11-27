package com.angel.ecomerce.service.products;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.OrderItemsRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.service.storage.IImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Ciclo de vida de la imagen de producto contra el bucket, sin Spring ni base de
 * datos: guardar, reemplazar y borrar.
 */
@ExtendWith(MockitoExtension.class)
class ProductsImageFlowTest {

    @Mock
    private ProductsRepository productsRepository;
    @Mock
    private CategoriesRepository categoriesRepository;
    @Mock
    private OrderItemsRepository orderItemsRepository;
    @Mock
    private IImageStorage imageStorage;

    private ProductsService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setPrefix("products");
        service = new ProductsService(productsRepository, categoriesRepository, orderItemsRepository,
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
    void updateWithNewImageDeletesThePreviousOne() {
        Product existente = new Product();
        existente.setId(5L);
        existente.setName("Libro de Java");
        existente.setDescription("Libro");
        existente.setPrice(new BigDecimal("19.99"));
        existente.setActive(true);
        existente.setCategory(categoria());
        existente.setImageKey("products/5/vieja.jpg");
        existente.setImageUrl("https://externa.example/vieja.jpg");

        Product cambios = productoNuevo(null);

        given(productsRepository.findById(5L)).willReturn(Optional.of(existente));
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(categoria()));
        given(imageStorage.store(anyString(), any())).willAnswer(invocation -> invocation.getArgument(0));
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        Product actualizado = service.updateById(5L, cambios, imagenPng());

        verify(imageStorage).delete("products/5/vieja.jpg");
        assertTrue(actualizado.getImageKey().startsWith("products/5/"));
        assertTrue(actualizado.getImageKey().endsWith(".png"));
        assertNull(actualizado.getImageUrl());
    }

    @Test
    void updateWithoutImageKeepsTheBucketObject() {
        Product existente = new Product();
        existente.setId(6L);
        existente.setName("Libro de Java");
        existente.setPrice(new BigDecimal("19.99"));
        existente.setActive(true);
        existente.setCategory(categoria());
        existente.setImageKey("products/6/intacta.jpg");

        Product cambios = productoNuevo("https://externa.example/otra.jpg");

        given(productsRepository.findById(6L)).willReturn(Optional.of(existente));
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(categoria()));
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        Product actualizado = service.updateById(6L, cambios, null);

        assertEquals("products/6/intacta.jpg", actualizado.getImageKey());
        verify(imageStorage, never()).delete(anyString());
        verify(imageStorage, never()).store(anyString(), any());
    }

    @Test
    void deleteByIdRemovesTheImageBeforeTheRow() {
        Product existente = new Product();
        existente.setId(7L);
        existente.setName("Libro");
        existente.setPrice(new BigDecimal("9.99"));
        existente.setImageKey("products/7/borrar.jpg");
        existente.setCategory(categoria());

        given(productsRepository.findById(7L)).willReturn(Optional.of(existente));
        given(orderItemsRepository.existsByProductId(7L)).willReturn(false);

        service.deleteById(7L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(imageStorage).delete("products/7/borrar.jpg");
        verify(productsRepository).delete(captor.capture());
        assertNull(captor.getValue().getImageKey());
    }

    @Test
    void removeImageClearsTheKeyWithoutDeletingTheProduct() {
        Product existente = new Product();
        existente.setId(8L);
        existente.setName("Libro");
        existente.setPrice(new BigDecimal("9.99"));
        existente.setImageKey("products/8/solo-imagen.jpg");
        existente.setCategory(categoria());

        given(productsRepository.findById(8L)).willReturn(Optional.of(existente));
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.removeImage(8L);

        verify(imageStorage).delete("products/8/solo-imagen.jpg");
        assertNull(existente.getImageKey());
    }

    @Test
    void removeImageIsIdempotentWhenThereIsNoImage() {
        Product existente = new Product();
        existente.setId(9L);
        existente.setName("Libro");
        existente.setPrice(new BigDecimal("9.99"));
        existente.setCategory(categoria());

        given(productsRepository.findById(9L)).willReturn(Optional.of(existente));

        service.removeImage(9L);

        verify(imageStorage, never()).delete(anyString());
        verify(productsRepository, never()).save(any(Product.class));
    }

    @Test
    void oversizedImageIsRejectedBeforeTouchingTheBucket() {
        Product product = productoNuevo(null);
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(11L);
            return p;
        });
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(categoria()));

        byte[] huge = new byte[(int) (6 * 1024 * 1024)];
        assertThrows(IllegalArgumentException.class,
                () -> service.save(product, new ImageUpload(huge, "image/png", "grande.png")));

        verify(imageStorage, never()).store(anyString(), any());
    }

    @Test
    void nonImageContentTypeIsRejected() {
        Product product = productoNuevo(null);
        given(productsRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(12L);
            return p;
        });
        given(categoriesRepository.findById(1L)).willReturn(Optional.of(categoria()));

        assertThrows(IllegalArgumentException.class,
                () -> service.save(product, new ImageUpload("hola".getBytes(), "application/pdf", "doc.pdf")));
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