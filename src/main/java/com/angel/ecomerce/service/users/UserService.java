package com.angel.ecomerce.service.users;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService implements IUser {
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UsersRepository usersRepository, OrdersRepository ordersRepository,
                       PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (user.getId() != null) {
            throw new IllegalArgumentException("Para actualizar un usuario use updateById");
        }

        String email = normalizeEmail(user.getEmail());
        validateEmail(email, null);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        user.setName(trim(user.getName()));
        user.setLastName(trim(user.getLastName()));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }
        user.setActive(true);
        if (user.getAddresses() == null) {
            user.setAddresses(new ArrayList<>());
        }
        user.getAddresses().forEach(address -> address.setUser(user));

        return usersRepository.save(user);
    }

    @Override
    public List<User> getAll() {
        return usersRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<User> getActive() {
        return usersRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return usersRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usersRepository.findByEmailIgnoreCase(email.trim());
    }

    @Override
    @Transactional
    public User updateById(Long id, User updatedUser) {
        requireId(id, "El id del usuario es obligatorio");
        if (updatedUser == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        return usersRepository.findById(id)
                .map(existingUser -> {
                    String email = normalizeEmail(updatedUser.getEmail());
                    validateEmail(email, id);

                    existingUser.setName(trim(updatedUser.getName()));
                    existingUser.setLastName(trim(updatedUser.getLastName()));
                    existingUser.setEmail(email);
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }
                    if (updatedUser.getRole() != null) {
                        existingUser.setRole(updatedUser.getRole());
                    }
                    existingUser.setActive(updatedUser.isActive());
                    return usersRepository.save(existingUser);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con ese id"));
    }

    @Override
    @Transactional
    public void deactivateById(Long id) {
        requireId(id, "El id del usuario es obligatorio");

        User user = usersRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe un usuario con ese id"));
        user.setActive(false);
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireId(id, "El id del usuario es obligatorio");

        if (!usersRepository.existsById(id)) {
            throw new NoSuchElementException("No existe un usuario con ese id");
        }
        if (ordersRepository.existsByUserId(id)) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene pedidos asociados; desactívelo en su lugar");
        }
        usersRepository.deleteById(id);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email, Long id) {
        usersRepository.findByEmailIgnoreCase(email)
                .filter(existing -> id == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese email");
                });
    }

    private void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
