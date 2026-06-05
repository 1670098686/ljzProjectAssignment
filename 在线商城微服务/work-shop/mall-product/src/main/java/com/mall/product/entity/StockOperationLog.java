package com.mall.product.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(
        name = "product_stock_operation_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_product_action", columnNames = {"order_id", "product_id", "action_type"})
        },
        indexes = {
                @Index(name = "idx_order_action", columnList = "order_id,action_type"),
                @Index(name = "idx_product_action", columnList = "product_id,action_type")
        }
)
public class StockOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "action_type", nullable = false, length = 16)
    private String actionType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
