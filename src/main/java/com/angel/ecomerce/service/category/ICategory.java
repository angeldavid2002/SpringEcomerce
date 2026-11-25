package com.angel.ecomerce.service.category;

import com.angel.ecomerce.entity.Category;

import java.util.List;
import java.util.Optional;

public interface ICategory {
    Category save(Category category);

    List<Category> getAll();

    List<Category> getActive();

    void deleteById(Long id);

    Optional<Category> findById(Long id);

    Optional<Category> findByName(String name);

    Category updateById(Long id, Category updatedCategory);
}
