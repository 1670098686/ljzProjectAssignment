package com.mall.stock.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "stock_stock")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private Long productId;
    
    private Integer availableQty;
    
    private Integer lockedQty;
    
    private Integer version;
    
    private Date updatedAt;
    
    @PrePersist
    protected void onCreate() {
        updatedAt = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
