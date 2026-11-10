package com.angel.ecomerce.service.category;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
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
        if (category == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        if (category.getId() != null) {
            throw new IllegalArgumentException("Para actualizar una categoría use updateById");
        }

        normalize(category);
        validate(category, null);
        category.setActive(true);
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
    public void deleteAll() {
        if (productsRepository.count() > 0) {
            throw new IllegalStateException("No se pueden eliminar las categorías mientras existan productos asociados");
        }
        categoriesRepository.deleteAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la categoría es obligatorio");
        }
        if (!categoriesRepository.existsById(id)) {
            throw new NoSuchElementException("No existe una categoría con ese id");
        }
        if (productsRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoriesRepository.deleteById(id);
    }

    @Override
    public Optional<Category> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
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
    public Category updateById(Long id, Category updatedCategory) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la categoría es obligatorio");
        }
        if (updatedCategory == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }

        return categoriesRepository.findById(id)
                .map(existingCategory -> {
                    normalize(updatedCategory);
                    validate(updatedCategory, id);

                    existingCategory.setName(updatedCategory.getName());
                    existingCategory.setDescription(updatedCategory.getDescription());
                    existingCategory.setActive(updatedCategory.isActive());
                    return categoriesRepository.save(existingCategory);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));
    }

    private void normalize(Category category) {
        category.setName(trim(category.getName()));
        category.setDescription(trim(category.getDescription()));
    }

    private void validate(Category category, Long id) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        categoriesRepository.findByNameIgnoreCase(category.getName())
                .filter(existing -> id == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
                });
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
