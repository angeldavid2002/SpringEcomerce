package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.product.ProductCreateRequest;
import com.angel.ecomerce.dto.product.ProductResponse;
import com.angel.ecomerce.dto.product.ProductUpdateRequest;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.products.IProduct;
import com.angel.ecomerce.service.storage.IImageStorage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private final IProduct productsService;
    private final OwnershipGuard ownership;
    private final IImageStorage imageStorage;

    public ProductsController(IProduct productsService, OwnershipGuard ownership, IImageStorage imageStorage) {
        this.productsService = productsService;
        this.ownership = ownership;
        this.imageStorage = imageStorage;
    }

    /**
     * Catálogo de la tienda. Sin token o con un token {@code CUSTOMER} solo
     * devuelve productos activos; un {@code ADMIN} ve también los inactivos.
     * Soporta paginación (?page=&limit=) y filtros de precio y rating.
     *
     * <p>{@code imageUrl} es una URL firmada y temporal, no la ruta del bucket.</p>
     */
    @GetMapping
    public Page<ProductResponse> getAll(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating) {

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("name").ascending());

        boolean isAdmin = ownership.isAdmin();

        if (minPrice != null || maxPrice != null) {
            if (minPrice == null) {
                minPrice = BigDecimal.ZERO;
            }
            if (maxPrice == null) {
                maxPrice = new BigDecimal("999999999.99");
            }
            // El público (y un CUSTOMER) solo ve activos; los filtros no
            // deberían filtrar al revés que el listado general.
            return (isAdmin
                    ? productsService.findByPriceBetween(minPrice, maxPrice, pageable)
                    : productsService.findActiveByPriceBetween(minPrice, maxPrice, pageable))
                    .map(this::toResponse);
        }

        if (minRating != null) {
            return (isAdmin
                    ? productsService.findByRatingGreaterThanEqual(minRating, pageable)
                    : productsService.findActiveByRatingGreaterThanEqual(minRating, pageable))
                    .map(this::toResponse);
        }

        if (isAdmin) {
            return productsService.getAll(pageable).map(this::toResponse);
        }
        return productsService.getActive(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productsService.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @GetMapping("/category/{categoryId}")
    public Page<ProductResponse> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("name").ascending());
        boolean isAdmin = ownership.isAdmin();

        if (isAdmin) {
            return productsService.getByCategory(categoryId, pageable).map(this::toResponse);
        }
        return productsService.getActiveByCategory(categoryId, pageable).map(this::toResponse);
    }

    @GetMapping("/search")
    public Page<ProductResponse> search(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("name").ascending());
        boolean isAdmin = ownership.isAdmin();

        if (isAdmin) {
            return productsService.searchByName(name, pageable).map(this::toResponse);
        }
        return productsService.searchActiveByName(name, pageable).map(this::toResponse);
    }

    /**
     * Crea un producto. En JSON no se envían imágenes; en
     * {@code multipart/form-data} el campo {@code image} se sube al bucket.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(productsService.save(request.toEntity())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createWithImage(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String imageUrl,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        Category category = new Category();
        category.setId(categoryId);
        product.setCategory(category);

        Product saved = productsService.save(product, toImageUpload(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * Actualiza un producto. Si llega imagen por multipart, la anterior se borra
     * del bucket y se guarda la nueva.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return toResponse(productsService.updateById(id, request.toEntity()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse updateWithImage(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String active,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setActive(Boolean.parseBoolean(active));
        Category category = new Category();
        category.setId(categoryId);
        product.setCategory(category);

        return toResponse(productsService.updateById(id, product, toImageUpload(image)));
    }

    /**
     * Quita solo la imagen del bucket; el producto se conserva.
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        productsService.removeImage(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable Long id) {
        productsService.deactivateById(id);
        return productsService.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.from(product, imageStorage::presignedUrl);
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(this::toResponse).toList();
    }

    private ImageUpload toImageUpload(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return ImageUpload.of(image.getBytes(), image.getContentType(), image.getOriginalFilename());
    }
}