package com.oms.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions", indexes = {
    @Index(name = "idx_inv_tx_order_id", columnList = "order_id"),
    @Index(name = "idx_inv_tx_product_id", columnList = "product_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // DEDUCT, REVERT

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
