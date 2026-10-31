package com.angel.ecomerce.service.products;

import com.angel.ecomerce.entity.Product;

import java.util.List;
import java.util.Optional;

public interface IProducts {
    Product save(Product product);

    List<Product> getAll();

    List<Product> getActive();

    List<Product> getByCategory(Long categoryId);

    List<Product> getActiveByCategory(Long categoryId);

    List<Product> searchByName(String name);

    Optional<Product> findById(Long id);

    Optional<Product> updateById(Long id, Product updatedProduct);

    String deactivateById(Long id);

    String deleteById(Long id);
}
