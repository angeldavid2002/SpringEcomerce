package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.category.CategoryCreateRequest;
import com.angel.ecomerce.dto.category.CategoryResponse;
import com.angel.ecomerce.dto.category.CategoryUpdateRequest;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.category.ICategory;
import com.angel.ecomerce.service.storage.IImageStorage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final ICategory categoryService;
    private final OwnershipGuard ownership;
    private final IImageStorage imageStorage;

    public CategoryController(ICategory categoryService, OwnershipGuard ownership, IImageStorage imageStorage) {
        this.categoryService = categoryService;
        this.ownership = ownership;
        this.imageStorage = imageStorage;
    }

    /**
     * Categorías de la tienda. Sin token o con un token {@code CUSTOMER} solo
     * devuelve las activas; un {@code ADMIN} ve también las inactivas.
     * Soporta paginación (?page=&limit=).
     *
     * <p>{@code imageUrl} es una URL firmada y temporal, no la ruta del bucket.</p>
     */
    @GetMapping
    public Page<CategoryResponse> getAll(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("name").ascending());
        List<Category> categories = ownership.isAdmin()
                ? categoryService.getAll()
                : categoryService.getActive();
        int start = (int) Math.min((long) page * limit, categories.size());
        int end = (int) Math.min((long) start + limit, categories.size());
        List<Category> paged = categories.subList(start, end);
        return new PageImpl<>(paged, pageable, categories.size())
                .map(this::toResponse);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));
    }

    @GetMapping("/by-name")
    public CategoryResponse getByName(@RequestParam String name) {
        return categoryService.findByName(name)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese nombre"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        Category category = categoryService.save(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(category));
    }

    /**
     * Crea una categoría con imagen. En {@code multipart/form-data} el campo
     * {@code image} se sube al bucket.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategoryResponse> createWithImage(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);

        Category saved = categoryService.save(category, toImageUpload(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return toResponse(categoryService.updateById(id, request.toEntity()));
    }

    /**
     * Actualiza una categoría. Si llega imagen por multipart, la anterior se
     * borra del bucket y se guarda la nueva.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryResponse updateWithImage(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String active,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setActive(Boolean.parseBoolean(active));

        return toResponse(categoryService.updateById(id, category, toImageUpload(image)));
    }

    /**
     * Quita solo la imagen del bucket; la categoría se conserva.
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        categoryService.removeImage(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.from(category, imageStorage::presignedUrl);
    }

    private ImageUpload toImageUpload(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return ImageUpload.of(image.getBytes(), image.getContentType(), image.getOriginalFilename());
    }
}
