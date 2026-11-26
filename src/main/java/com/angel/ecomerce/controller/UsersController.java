package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.user.UserCreateRequest;
import com.angel.ecomerce.dto.user.UserPatchRequest;
import com.angel.ecomerce.dto.user.UserResponse;
import com.angel.ecomerce.dto.user.UserUpdateRequest;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.users.IUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final IUser usersService;
    private final OwnershipGuard ownership;

    public UsersController(IUser usersService, OwnershipGuard ownership) {
        this.usersService = usersService;
        this.ownership = ownership;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return usersService.getAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        Long userId = ownership.currentUserId();
        return usersService.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe el usuario autenticado"));
    }

    @PatchMapping("/me")
    public UserResponse patchMe(@Valid @RequestBody UserPatchRequest request) {
        Long userId = ownership.currentUserId();
        UserResponse updated = usersService.patchById(userId, request)
                .map(UserResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe el usuario autenticado"));
        return updated;
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
