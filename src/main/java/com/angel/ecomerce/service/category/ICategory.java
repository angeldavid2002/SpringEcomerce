package com.angel.ecomerce.service.category;

import com.angel.ecomerce.entity.Category;

import java.util.List;
import java.util.Optional;

public interface ICategory{
    Category save(Category category);

    List<Category> getAll();

    List<Category> deleteAll();
    String deleteById(Long id);

    Optional<Category> findById(Long id);

    Optional<Category> updateById(Long id,Category updatedCategory);

}
