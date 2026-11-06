package com.mall.order.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private Long productId;
    
    private Double price;
    
    private Integer quantity;
    
    private Double amount;
    
    private String productName;
    
    private String productImage;
}
