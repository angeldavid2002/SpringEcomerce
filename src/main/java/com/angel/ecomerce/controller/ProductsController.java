package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.product.ProductCreateRequest;
import com.angel.ecomerce.dto.product.ProductResponse;
import com.angel.ecomerce.dto.product.ProductUpdateRequest;
import com.angel.ecomerce.service.products.IProduct;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private final IProduct productsService;

    public ProductsController(IProduct productsService) {
        this.productsService = productsService;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productsService.getAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/active")
    public List<ProductResponse> getActive() {
        return productsService.getActive().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productsService.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getByCategory(@PathVariable Long categoryId) {
        return productsService.getByCategory(categoryId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/category/{categoryId}/active")
    public List<ProductResponse> getActiveByCategory(@PathVariable Long categoryId) {
        return productsService.getActiveByCategory(categoryId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam String name) {
        return productsService.searchActiveByName(name).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponse.from(productsService.save(request.toEntity())));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ProductResponse.from(productsService.updateById(id, request.toEntity()));
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivate(@PathVariable Long id) {
        productsService.deactivateById(id);
        return productsService.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
