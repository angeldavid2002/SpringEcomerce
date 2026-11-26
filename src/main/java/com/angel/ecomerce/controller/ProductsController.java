package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.product.ProductCreateRequest;
import com.angel.ecomerce.dto.product.ProductResponse;
import com.angel.ecomerce.dto.product.ProductUpdateRequest;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.products.IProduct;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
     * Soporta paginación (?page=&limit=) y filtros de precio y rating.
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
            return productsService.findByPriceBetween(minPrice, maxPrice, pageable)
                    .map(ProductResponse::from);
        }

        if (minRating != null) {
            return productsService.findByRatingGreaterThanEqual(minRating, pageable)
                    .map(ProductResponse::from);
        }

        if (isAdmin) {
            return productsService.getAll(pageable).map(ProductResponse::from);
        }
        return productsService.getActive(pageable).map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productsService.findById(id)
                .map(ProductResponse::from)
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
            return productsService.getByCategory(categoryId, pageable).map(ProductResponse::from);
        }
        return productsService.getActiveByCategory(categoryId, pageable).map(ProductResponse::from);
    }

    @GetMapping("/search")
    public Page<ProductResponse> search(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        PageRequest pageable = PageRequest.of(page, limit, Sort.by("name").ascending());
        boolean isAdmin = ownership.isAdmin();

        if (isAdmin) {
            return productsService.searchByName(name, pageable).map(ProductResponse::from);
        }
        return productsService.searchActiveByName(name, pageable).map(ProductResponse::from);
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
