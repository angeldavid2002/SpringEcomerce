package com.angel.ecomerce.service.products;

import com.angel.ecomerce.entity.Product;

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

    Optional<Product> findById(Long id);

    Product updateById(Long id, Product updatedProduct);

    void deactivateById(Long id);

    void deleteById(Long id);
}
