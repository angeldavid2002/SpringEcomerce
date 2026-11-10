package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductsRepository extends JpaRepository<Product, Long> {
    boolean existsByCategoryId(Long categoryId);

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Product> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);

    List<Product> findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);
}
