package com.angel.ecomerce.service.products;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.repository.CategoriesRepository;
import com.angel.ecomerce.repository.OrderItemsRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductsService implements IProduct {
    private final ProductsRepository productsRepository;
    private final CategoriesRepository categoriesRepository;
    private final OrderItemsRepository orderItemsRepository;

    public ProductsService(ProductsRepository productsRepository,
                           CategoriesRepository categoriesRepository,
                           OrderItemsRepository orderItemsRepository) {
        this.productsRepository = productsRepository;
        this.categoriesRepository = categoriesRepository;
        this.orderItemsRepository = orderItemsRepository;
    }

    @Override
    @Transactional
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (product.getId() != null) {
            throw new IllegalArgumentException("Para actualizar un producto use updateById");
        }

        normalize(product);
        validate(product);
        product.setCategory(resolveCategory(product.getCategory()));
        product.setPrice(scalePrice(product.getPrice()));
        product.setActive(true);
        return productsRepository.save(product);
    }

    @Override
    public List<Product> getAll() {
        return productsRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<Product> getActive() {
        return productsRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public List<Product> getByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        return productsRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }

    @Override
    public List<Product> getActiveByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        return productsRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId);
    }

    @Override
    public List<Product> searchActiveByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return productsRepository.findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(name.trim());
    }

    @Override
    public Optional<Product> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return productsRepository.findById(id);
    }

    @Override
    @Transactional
    public Product updateById(Long id, Product updatedProduct) {
        if (id == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
        if (updatedProduct == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }

        return productsRepository.findById(id)
                .map(existingProduct -> {
                    normalize(updatedProduct);
                    validate(updatedProduct);
                    Category category = resolveCategory(updatedProduct.getCategory());

                    existingProduct.setName(updatedProduct.getName());
                    existingProduct.setDescription(updatedProduct.getDescription());
                    existingProduct.setPrice(scalePrice(updatedProduct.getPrice()));
                    existingProduct.setImageUrl(updatedProduct.getImageUrl());
                    existingProduct.setActive(updatedProduct.isActive());
                    existingProduct.setCategory(category);
                    return productsRepository.save(existingProduct);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
    }

    @Override
    @Transactional
    public void deactivateById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }

        Product product = productsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe un producto con ese id"));
        product.setActive(false);
        productsRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
        if (!productsRepository.existsById(id)) {
            throw new NoSuchElementException("No existe un producto con ese id");
        }
        if (orderItemsRepository.existsByProductId(id)) {
            throw new IllegalStateException("No se puede eliminar el producto porque está asociado a pedidos; desactívelo en su lugar");
        }
        productsRepository.deleteById(id);
    }

    private Category resolveCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("La categoría del producto es obligatoria");
        }
        return categoriesRepository.findById(category.getId())
                .orElseThrow(() -> new NoSuchElementException("No existe la categoría con id " + category.getId()));
    }

    private void normalize(Product product) {
        product.setName(trim(product.getName()));
        product.setDescription(trim(product.getDescription()));
        product.setImageUrl(trim(product.getImageUrl()));
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del producto debe ser mayor o igual a cero");
        }
    }

    private BigDecimal scalePrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
