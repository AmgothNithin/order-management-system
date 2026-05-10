package com.oms.inventory.repository;

import com.oms.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findBySku(String sku);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(@Param("id") String id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void incrementStock(@Param("id") String id, @Param("qty") int qty);
}
