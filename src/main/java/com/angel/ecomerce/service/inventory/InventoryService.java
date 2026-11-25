package com.angel.ecomerce.service.inventory;

import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.repository.InventoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InventoryService implements IInventory {
    private final InventoriesRepository inventoriesRepository;
    private final ProductsRepository productsRepository;

    public InventoryService(InventoriesRepository inventoriesRepository,
                            ProductsRepository productsRepository) {
        this.inventoriesRepository = inventoriesRepository;
        this.productsRepository = productsRepository;
    }

    @Override
    @Transactional
    public Inventory save(Long productId, Inventory inventory) {
        if (productId == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (inventory == null) {
            throw new IllegalArgumentException("El inventario es obligatorio");
        }
        if (inventory.getId() != null) {
            throw new IllegalArgumentException("Para actualizar un inventario use updateById");
        }

        Product product = productsRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("No existe el producto con id " + productId));
        if (inventoriesRepository.existsByProductId(productId)) {
            throw new IllegalArgumentException("El producto ya tiene un inventario registrado");
        }

        validate(inventory);
        inventory.setProduct(product);
        product.setInventory(inventory);
        return inventoriesRepository.save(inventory);
    }

    @Override
    public List<Inventory> getAll() {
        return inventoriesRepository.findAllByOrderByProductNameAsc();
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return inventoriesRepository.findById(id);
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return inventoriesRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public Inventory updateById(Long id, Inventory updatedInventory) {
        if (id == null) {
            throw new IllegalArgumentException("El id del inventario es obligatorio");
        }
        if (updatedInventory == null) {
            throw new IllegalArgumentException("El inventario es obligatorio");
        }

        return inventoriesRepository.findById(id)
                .map(existingInventory -> {
                    validate(updatedInventory);

                    existingInventory.setStock(updatedInventory.getStock());
                    existingInventory.setMinStock(updatedInventory.getMinStock());
                    return inventoriesRepository.save(existingInventory);
                })
                .orElseThrow(() -> new NoSuchElementException("No existe un inventario con ese id"));
    }

    @Override
    @Transactional
    public Inventory increaseStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a incrementar debe ser mayor a cero");
        }

        Inventory inventory = getByProductIdOrThrow(productId);
        inventory.setStock(inventory.getStock() + quantity);
        return inventoriesRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory decreaseStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero");
        }

        Inventory inventory = getByProductIdOrThrow(productId);
        if (inventory.getStock() < quantity) {
            throw new IllegalStateException("Stock insuficiente para el producto con id " + productId);
        }
        inventory.setStock(inventory.getStock() - quantity);
        return inventoriesRepository.save(inventory);
    }

    @Override
    public List<Inventory> getLowStock() {
        return inventoriesRepository.findLowStock();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del inventario es obligatorio");
        }
        if (!inventoriesRepository.existsById(id)) {
            throw new NoSuchElementException("No existe un inventario con ese id");
        }
        inventoriesRepository.deleteById(id);
    }

    private Inventory getByProductIdOrThrow(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        return inventoriesRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("No existe inventario para el producto con id " + productId));
    }

    private void validate(Inventory inventory) {
        if (inventory.getStock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        if (inventory.getMinStock() < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
    }
}
