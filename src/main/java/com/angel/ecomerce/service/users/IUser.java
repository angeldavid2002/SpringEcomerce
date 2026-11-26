package com.angel.ecomerce.service.users;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.dto.user.UserPatchRequest;

import java.util.List;
import java.util.Optional;

public interface IUser {
    User save(User user);

    List<User> getAll();

    List<User> getActive();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    User updateById(Long id, User updatedUser);

    Optional<User> patchById(Long id, UserPatchRequest patch);

    void deactivateById(Long id);

    void deleteById(Long id);
}
