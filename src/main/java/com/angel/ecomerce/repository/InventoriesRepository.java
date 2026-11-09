package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoriesRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    List<Inventory> findAllByOrderByProductNameAsc();

    @Query("select i from Inventory i where i.stock <= i.minStock order by i.product.name asc")
    List<Inventory> findLowStock();
}
