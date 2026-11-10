package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductsRepository extends JpaRepository<Product, Long> {
    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);
}
