package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductsRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByActiveTrue();

    List<Product> findByNameContainingIgnoreCase(String name);

    boolean existsByCategoryId(Long categoryId);
}
