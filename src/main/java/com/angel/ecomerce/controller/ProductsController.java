package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.product.ProductCreateRequest;
import com.angel.ecomerce.dto.product.ProductResponse;
import com.angel.ecomerce.dto.product.ProductUpdateRequest;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.security.OwnershipGuard;
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
    private final OwnershipGuard ownership;

    public ProductsController(IProduct productsService, OwnershipGuard ownership) {
        this.productsService = productsService;
        this.ownership = ownership;
    }

    /**
     * Catálogo de la tienda. Sin token o con un token {@code CUSTOMER} solo
     * devuelve productos activos; un {@code ADMIN} ve también los inactivos.
     */
    @GetMapping
    public List<ProductResponse> getAll() {
        return toResponses(isAdmin() ? productsService.getAll() : productsService.getActive());
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productsService.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getByCategory(@PathVariable Long categoryId) {
        return toResponses(isAdmin()
                ? productsService.getByCategory(categoryId)
                : productsService.getActiveByCategory(categoryId));
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam String name) {
        return toResponses(isAdmin()
                ? productsService.searchByName(name)
                : productsService.searchActiveByName(name));
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

    private boolean isAdmin() {
        return ownership.isAdmin();
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(ProductResponse::from).toList();
    }
}
