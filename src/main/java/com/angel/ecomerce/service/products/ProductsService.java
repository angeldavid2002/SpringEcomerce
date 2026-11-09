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
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductsService implements IProducts {
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
        validate(product);
        product.setCategory(resolveCategory(product.getCategory()));
        product.setPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
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
        return productsRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }

    @Override
    public List<Product> getActiveByCategory(Long categoryId) {
        return productsRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId);
    }

    @Override
    public List<Product> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return productsRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productsRepository.findById(id);
    }

    @Override
    @Transactional
    public Optional<Product> updateById(Long id, Product updatedProduct) {
        validate(updatedProduct);
        Category category = resolveCategory(updatedProduct.getCategory());

        return productsRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(updatedProduct.getName());
                    existingProduct.setDescription(updatedProduct.getDescription());
                    existingProduct.setPrice(updatedProduct.getPrice().setScale(2, RoundingMode.HALF_UP));
                    existingProduct.setImageUrl(updatedProduct.getImageUrl());
                    existingProduct.setActive(updatedProduct.isActive());
                    existingProduct.setCategory(category);
                    return productsRepository.save(existingProduct);
                });
    }

    @Override
    @Transactional
    public String deactivateById(Long id) {
        return productsRepository.findById(id)
                .map(product -> {
                    product.setActive(false);
                    productsRepository.save(product);
                    return "Producto desactivado exitosamente";
                })
                .orElse("No existe un producto con ese id");
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        if (!productsRepository.existsById(id)) {
            return "No existe un producto con ese id";
        }
        if (orderItemsRepository.existsByProductId(id)) {
            return "No se puede eliminar el producto porque está asociado a pedidos; desactívelo en su lugar";
        }
        productsRepository.deleteById(id);
        return "Se eliminó exitosamente";
    }

    private Category resolveCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("La categoría del producto es obligatoria");
        }
        return categoriesRepository.findById(category.getId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la categoría con id " + category.getId()));
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
}
