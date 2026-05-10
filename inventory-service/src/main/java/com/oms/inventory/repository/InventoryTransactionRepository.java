package com.oms.inventory.repository;

import com.oms.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
    boolean existsByOrderIdAndProductId(String orderId, String productId);
    List<InventoryTransaction> findByOrderId(String orderId);
}
