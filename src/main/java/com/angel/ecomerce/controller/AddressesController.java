package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.address.AddressCreateRequest;
import com.angel.ecomerce.dto.address.AddressResponse;
import com.angel.ecomerce.dto.address.AddressUpdateRequest;
import com.angel.ecomerce.service.address.IAddress;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/addresses")
public class AddressesController {
    private final IAddress addressService;

    public AddressesController(IAddress addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> getAll() {
        return addressService.getAll().stream()
                .map(AddressResponse::from)
                .toList();
    }

    @GetMapping("/user/{userId}")
    public List<AddressResponse> getByUser(@PathVariable Long userId) {
        return addressService.getByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AddressResponse getById(@PathVariable Long id) {
        return addressService.findById(id)
                .map(AddressResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe una dirección con ese id"));
    }

    @GetMapping("/{id}/user/{userId}")
    public AddressResponse getByIdAndUser(@PathVariable Long id, @PathVariable Long userId) {
        return addressService.findByIdAndUserId(id, userId)
                .map(AddressResponse::from)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe la dirección con id " + id + " para el usuario " + userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @RequestParam Long userId,
            @Valid @RequestBody AddressCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AddressResponse.from(addressService.save(userId, request.toEntity())));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressUpdateRequest request) {
        return AddressResponse.from(addressService.updateById(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
