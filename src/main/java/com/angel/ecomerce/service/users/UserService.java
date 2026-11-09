package com.angel.ecomerce.service.users;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.OrdersRepository;
import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService implements IUsers {
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;

    public UserService(UsersRepository usersRepository, OrdersRepository ordersRepository) {
        this.usersRepository = usersRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() != null) {
            throw new IllegalArgumentException("Para actualizar un usuario use updateById");
        }

        String email = normalizeEmail(user.getEmail());
        validateEmail(email, null);

        user.setEmail(email);
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
    public Optional<User> updateById(Long id, User updatedUser) {
        String email = normalizeEmail(updatedUser.getEmail());
        validateEmail(email, id);

        return usersRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(updatedUser.getName());
                    existingUser.setLastName(updatedUser.getLastName());
                    existingUser.setEmail(email);
                    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                        existingUser.setPassword(updatedUser.getPassword());
                    }
                    if (updatedUser.getRole() != null) {
                        existingUser.setRole(updatedUser.getRole());
                    }
                    existingUser.setActive(updatedUser.isActive());
                    return usersRepository.save(existingUser);
                });
    }

    @Override
    @Transactional
    public String deactivateById(Long id) {
        return usersRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    usersRepository.save(user);
                    return "Usuario desactivado exitosamente";
                })
                .orElse("No existe un usuario con ese id");
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        if (!usersRepository.existsById(id)) {
            return "No existe un usuario con ese id";
        }
        if (ordersRepository.existsByUserId(id)) {
            return "No se puede eliminar el usuario porque tiene pedidos asociados; desactívelo en su lugar";
        }
        usersRepository.deleteById(id);
        return "Se eliminó exitosamente";
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
}
