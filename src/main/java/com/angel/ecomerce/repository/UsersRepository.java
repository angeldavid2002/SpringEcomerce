package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<User,Long> {
}
