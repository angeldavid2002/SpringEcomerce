package com.angel.ecomerce.service.category;

import com.angel.ecomerce.config.StorageProperties;
import com.angel.ecomerce.dto.storage.ImageUpload;
import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import com.angel.ecomerce.service.storage.IImageStorage;
import com.angel.ecomerce.service.storage.ImageRules;
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
    private final IImageStorage imageStorage;
    private final StorageProperties storageProperties;

    public CategoryService(CategoriesRepository categoriesRepository,
                           ProductsRepository productsRepository,
                           IImageStorage imageStorage,
                           StorageProperties storageProperties) {
        this.categoriesRepository = categoriesRepository;
        this.productsRepository = productsRepository;
        this.imageStorage = imageStorage;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        return save(category, null);
    }

    @Override
    @Transactional
    public Category save(Category category, ImageUpload image) {
        if (category == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        if (category.getId() != null) {
            throw new IllegalArgumentException("Para actualizar una categoría use updateById");
        }

        normalize(category);
        validate(category, null);
        category.setActive(true);

        Category saved = categoriesRepository.save(category);
        if (image != null) {
            storeImage(saved, image);
        }
        return saved;
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
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la categoría es obligatorio");
        }
        Category category = categoriesRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));
        if (productsRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque tiene productos asociados");
        }

        // La imagen se borra del bucket antes que la fila.
        if (category.getImageKey() != null && !category.getImageKey().isBlank()) {
            imageStorage.delete(category.getImageKey());
        }
        categoriesRepository.delete(category);
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
        return updateById(id, updatedCategory, null);
    }

    @Override
    @Transactional
    public Category updateById(Long id, Category updatedCategory, ImageUpload image) {
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
                    if (image != null) {
                        replaceImage(existingCategory, image);
                    }
                    return categoriesRepository.save(existingCategory);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));
    }

    /** Borra solo la imagen del bucket y deja la categoría sin imagen. */
    @Override
    @Transactional
    public void removeImage(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la categoría es obligatorio");
        }
        Category category = categoriesRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe una categoría con ese id"));

        if (category.getImageKey() != null && !category.getImageKey().isBlank()) {
            imageStorage.delete(category.getImageKey());
            category.setImageKey(null);
            categoriesRepository.save(category);
        }
    }

    private void storeImage(Category category, ImageUpload image) {
        ImageRules.requireValid(image, storageProperties);
        String key = ImageRules.keyFor(storageProperties.getCategoryPrefix(), category.getId(), image);
        imageStorage.store(key, image);
        category.setImageKey(key);
        categoriesRepository.save(category);
    }

    private void replaceImage(Category category, ImageUpload image) {
        String previousKey = category.getImageKey();
        if (previousKey != null && !previousKey.isBlank()) {
            imageStorage.delete(previousKey);
        }
        storeImage(category, image);
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
