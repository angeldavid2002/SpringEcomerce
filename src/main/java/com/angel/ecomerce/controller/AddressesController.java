package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.address.AddressCreateRequest;
import com.angel.ecomerce.dto.address.AddressResponse;
import com.angel.ecomerce.dto.address.AddressUpdateRequest;
import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.security.OwnershipGuard;
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
    private final OwnershipGuard ownership;

    public AddressesController(IAddress addressService, OwnershipGuard ownership) {
        this.addressService = addressService;
        this.ownership = ownership;
    }

    @GetMapping
    public List<AddressResponse> getAll() {
        return addressService.getAll().stream()
                .map(AddressResponse::from)
                .toList();
    }

    @GetMapping("/me")
    public List<AddressResponse> getMyAddresses() {
        Long userId = ownership.currentUserId();
        return addressService.getByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AddressResponse getById(@PathVariable Long id) {
        return AddressResponse.from(requireOwnedAddress(id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @RequestParam Long userId,
            @Valid @RequestBody AddressCreateRequest request) {
        ownership.requireSelfOrAdmin(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AddressResponse.from(addressService.save(userId, request.toEntity())));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressUpdateRequest request) {
        requireOwnedAddress(id);
        return AddressResponse.from(addressService.updateById(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requireOwnedAddress(id);
        addressService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Address requireOwnedAddress(Long id) {
        Address address = addressService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe una dirección con ese id"));
        ownership.requireOwnerOrAdmin(address.getUser().getId());
        return address;
    }
}
