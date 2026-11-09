package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByOrderByNameAsc();

    List<User> findByActiveTrueOrderByNameAsc();
}
