package com.angel.ecomerce.service.category;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.repository.CategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService implements ICategory{
    @Autowired
    private CategoriesRepository categoriesRepository;

    @Override
    public Category save(Category category) {
        return categoriesRepository.save(category);
    }

    @Override
    public List<Category> getAll() {
        return categoriesRepository.findAll();
    }

    @Override
    public List<Category> deleteAll() {
        categoriesRepository.deleteAll();
        return categoriesRepository.findAll();
    }

    @Override
    public String deleteById(Long id) {
        if (categoriesRepository.existsById(id)){
            categoriesRepository.deleteById(id);
            return "Se elimino exitosamente";
        }
        return "No existen elementos para eliminar con ese id";
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoriesRepository.findById(id);
    }

    @Override
    public Optional<Category> updateById(Long id, Category updatedCategory) {
        return categoriesRepository.findById(id)
                .map(existingCategory -> {
                    existingCategory.setName(updatedCategory.getName());
                    existingCategory.setDescription(updatedCategory.getDescription());
                    existingCategory.setActive(updatedCategory.isActive());
                    return categoriesRepository.save(existingCategory);
                });
    }

}
