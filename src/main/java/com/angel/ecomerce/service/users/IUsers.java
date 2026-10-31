package com.angel.ecomerce.service.users;

import com.angel.ecomerce.entity.User;

import java.util.List;
import java.util.Optional;

public interface IUsers {
    User save(User user);

    List<User> getAll();

    List<User> getActive();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> updateById(Long id, User updatedUser);

    String deactivateById(Long id);

    String deleteById(Long id);
}
