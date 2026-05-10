package com.oms.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_sku", columnList = "sku"),
    @Index(name = "idx_products_category", columnList = "category")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    private String description;
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "reserved_quantity")
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Integer getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
