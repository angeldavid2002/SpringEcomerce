package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.inventory.InventoryCreateRequest;
import com.angel.ecomerce.dto.inventory.InventoryResponse;
import com.angel.ecomerce.dto.inventory.InventoryUpdateRequest;
import com.angel.ecomerce.service.inventory.IInventory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/inventories")
public class InventoryController {
    private final IInventory inventoryService;

    public InventoryController(IInventory inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> getAll() {
        return inventoryService.getAll().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InventoryResponse getById(@PathVariable Long id) {
        return inventoryService.findById(id)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un inventario con ese id"));
    }

    @GetMapping("/product/{productId}")
    public InventoryResponse getByProduct(@PathVariable Long productId) {
        return inventoryService.findByProductId(productId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe inventario para ese producto"));
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> getLowStock() {
        return inventoryService.getLowStock().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @GetMapping("/product/{productId}/low-stock")
    public boolean isLowStock(@PathVariable Long productId) {
        return inventoryService.isLowStock(productId);
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(
            @RequestParam Long productId,
            @Valid @RequestBody InventoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryResponse.from(inventoryService.save(productId, request.toEntity())));
    }

    @PutMapping("/{id}")
    public InventoryResponse update(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        return InventoryResponse.from(inventoryService.updateById(id, request.toEntity()));
    }

    @PatchMapping("/product/{productId}/increase")
    public InventoryResponse increase(@PathVariable Long productId, @RequestParam int quantity) {
        return InventoryResponse.from(inventoryService.increaseStock(productId, quantity));
    }

    @PatchMapping("/product/{productId}/decrease")
    public InventoryResponse decrease(@PathVariable Long productId, @RequestParam int quantity) {
        return InventoryResponse.from(inventoryService.decreaseStock(productId, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Void> deleteByProduct(@PathVariable Long productId) {
        inventoryService.deleteByProductId(productId);
        return ResponseEntity.noContent().build();
    }
}
