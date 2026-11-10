package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.user.UserCreateRequest;
import com.angel.ecomerce.dto.user.UserResponse;
import com.angel.ecomerce.dto.user.UserUpdateRequest;
import com.angel.ecomerce.service.users.IUsers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final IUsers usersService;

    public UsersController(IUsers usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return usersService.getAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/active")
    public List<UserResponse> getActive() {
        return usersService.getActive().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return usersService.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con ese id"));
    }

    @GetMapping("/by-email")
    public UserResponse getByEmail(@RequestParam String email) {
        return usersService.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con ese email"));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(usersService.save(request.toEntity())));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(usersService.updateById(id, request.toEntity()));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        usersService.deactivateById(id);
        return usersService.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con ese id"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usersService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
