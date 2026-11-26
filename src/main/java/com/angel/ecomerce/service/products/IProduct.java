package com.angel.ecomerce.service.products;

import com.angel.ecomerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IProduct {
    Product save(Product product);

    List<Product> getAll();

    List<Product> getActive();

    List<Product> getByCategory(Long categoryId);

    List<Product> getActiveByCategory(Long categoryId);

    List<Product> searchActiveByName(String name);

    List<Product> searchByName(String name);

    Page<Product> getAll(Pageable pageable);

    Page<Product> getActive(Pageable pageable);

    Page<Product> getByCategory(Long categoryId, Pageable pageable);

    Page<Product> getActiveByCategory(Long categoryId, Pageable pageable);

    Page<Product> searchByName(String name, Pageable pageable);

    Page<Product> searchActiveByName(String name, Pageable pageable);

    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> findByRatingGreaterThanEqual(BigDecimal minRating, Pageable pageable);

    Optional<Product> findById(Long id);

    Product updateById(Long id, Product updatedProduct);

    void deactivateById(Long id);

    void deleteById(Long id);
}
