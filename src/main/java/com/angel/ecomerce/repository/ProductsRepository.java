package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Product,Long> {
}
