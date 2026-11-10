package com.angel.ecomerce.repository;

import com.angel.ecomerce.entity.Inventory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoriesRepository extends JpaRepository<Inventory, Long> {
    @EntityGraph(attributePaths = "product")
    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @EntityGraph(attributePaths = "product")
    List<Inventory> findAllByOrderByProductNameAsc();

    @EntityGraph(attributePaths = "product")
    @Query("select i from Inventory i where i.stock <= i.minStock order by i.product.name asc")
    List<Inventory> findLowStock();

    @Override
    @EntityGraph(attributePaths = "product")
    Optional<Inventory> findById(Long id);
}
