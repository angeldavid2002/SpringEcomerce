package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriesRepository extends JpaRepository<Category,Long> {
}
