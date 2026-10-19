package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
}
