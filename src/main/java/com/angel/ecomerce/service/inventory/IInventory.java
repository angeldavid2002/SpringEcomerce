package com.angel.ecomerce.service.inventory;

import com.angel.ecomerce.entity.Inventory;

import java.util.List;
import java.util.Optional;

public interface IInventory {
    Inventory save(Long productId, Inventory inventory);

    List<Inventory> getAll();

    Optional<Inventory> findById(Long id);

    Optional<Inventory> findByProductId(Long productId);

    Inventory updateById(Long id, Inventory updatedInventory);

    Inventory increaseStock(Long productId, int quantity);

    Inventory decreaseStock(Long productId, int quantity);

    List<Inventory> getLowStock();

    void deleteById(Long id);
}
