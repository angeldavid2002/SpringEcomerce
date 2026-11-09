package com.angel.ecomerce.service.category;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryService implements ICategory {
    private final CategoriesRepository categoriesRepository;
    private final ProductsRepository productsRepository;

    public CategoryService(CategoriesRepository categoriesRepository, ProductsRepository productsRepository) {
        this.categoriesRepository = categoriesRepository;
        this.productsRepository = productsRepository;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        validate(category, category.getId());
        return categoriesRepository.save(category);
    }

    @Override
    public List<Category> getAll() {
        return categoriesRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<Category> getActive() {
        return categoriesRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional
    public List<Category> deleteAll() {
        if (productsRepository.count() > 0) {
            throw new IllegalStateException("No se pueden eliminar las categorías mientras existan productos asociados");
        }
        categoriesRepository.deleteAll();
        return categoriesRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        if (!categoriesRepository.existsById(id)) {
            return "No existen elementos para eliminar con ese id";
        }
        if (productsRepository.existsByCategoryId(id)) {
            return "No se puede eliminar la categoría porque tiene productos asociados";
        }
        categoriesRepository.deleteById(id);
        return "Se eliminó exitosamente";
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoriesRepository.findById(id);
    }

    @Override
    public Optional<Category> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return categoriesRepository.findByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional
    public Optional<Category> updateById(Long id, Category updatedCategory) {
        return categoriesRepository.findById(id)
                .map(existingCategory -> {
                    validate(updatedCategory, id);
                    existingCategory.setName(updatedCategory.getName());
                    existingCategory.setDescription(updatedCategory.getDescription());
                    existingCategory.setActive(updatedCategory.isActive());
                    return categoriesRepository.save(existingCategory);
                });
    }

    private void validate(Category category, Long id) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        categoriesRepository.findByNameIgnoreCase(category.getName().trim())
                .filter(existing -> id == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
                });
    }
}
