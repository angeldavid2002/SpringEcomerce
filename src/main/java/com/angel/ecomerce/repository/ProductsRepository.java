package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Products,Long> {
}
