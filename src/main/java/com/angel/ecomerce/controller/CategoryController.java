package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.category.CategoryCreateRequest;
import com.angel.ecomerce.dto.category.CategoryResponse;
import com.angel.ecomerce.dto.category.CategoryUpdateRequest;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.category.ICategory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final ICategory categoryService;
    private final OwnershipGuard ownership;

    public CategoryController(ICategory categoryService, OwnershipGuard ownership) {
        this.categoryService = categoryService;
        this.ownership = ownership;
    }

    /**
     * Categorías de la tienda. Sin token o con un token {@code CUSTOMER} solo
     * devuelve las activas; un {@code ADMIN} ve también las inactivas.
     */
    @GetMapping
    public List<CategoryResponse> getAll() {
        List<Category> categories = ownership.isAdmin()
                ? categoryService.getAll()
                : categoryService.getActive();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));
    }

    @GetMapping("/by-name")
    public CategoryResponse getByName(@RequestParam String name) {
        return categoryService.findByName(name)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese nombre"));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        Category category = categoryService.save(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return CategoryResponse.from(categoryService.updateById(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
