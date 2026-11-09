package com.angel.ecomerce.service.inventory;

import com.angel.ecomerce.entity.Inventory;
import com.angel.ecomerce.entity.Product;
import com.angel.ecomerce.repository.InventoriesRepository;
import com.angel.ecomerce.repository.ProductsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        Product product = productsRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto con id " + productId));
        if (inventoriesRepository.existsByProductId(productId)) {
            throw new IllegalArgumentException("El producto ya tiene un inventario registrado");
        }

        validate(inventory);
        inventory.setId(null);
        inventory.setProduct(product);
        return inventoriesRepository.save(inventory);
    }

    @Override
    public List<Inventory> getAll() {
        return inventoriesRepository.findAllByOrderByProductNameAsc();
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        return inventoriesRepository.findById(id);
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoriesRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public Optional<Inventory> updateById(Long id, Inventory updatedInventory) {
        validate(updatedInventory);

        return inventoriesRepository.findById(id)
                .map(existingInventory -> {
                    existingInventory.setStock(updatedInventory.getStock());
                    existingInventory.setMinStock(updatedInventory.getMinStock());
                    return inventoriesRepository.save(existingInventory);
                });
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
            throw new IllegalArgumentException("Stock insuficiente para el producto con id " + productId);
        }
        inventory.setStock(inventory.getStock() - quantity);
        return inventoriesRepository.save(inventory);
    }

    @Override
    public boolean isLowStock(Long productId) {
        Inventory inventory = getByProductIdOrThrow(productId);
        return inventory.getStock() <= inventory.getMinStock();
    }

    @Override
    public List<Inventory> getLowStock() {
        return inventoriesRepository.findLowStock();
    }

    @Override
    @Transactional
    public String deleteById(Long id) {
        if (!inventoriesRepository.existsById(id)) {
            return "No existe un inventario con ese id";
        }
        inventoriesRepository.deleteById(id);
        return "Se eliminó exitosamente";
    }

    @Override
    @Transactional
    public String deleteByProductId(Long productId) {
        Optional<Inventory> inventory = inventoriesRepository.findByProductId(productId);
        if (inventory.isEmpty()) {
            return "No existe un inventario para ese producto";
        }
        inventoriesRepository.delete(inventory.get());
        return "Se eliminó exitosamente";
    }

    private Inventory getByProductIdOrThrow(Long productId) {
        return inventoriesRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("No existe inventario para el producto con id " + productId));
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
