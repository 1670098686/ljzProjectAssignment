package com.mall.order.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "order_order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderNo;
    
    @Column(nullable = false)
    private Long userId;
    
    private Double totalAmount;
    
    private Double freight;
    
    private Double finalAmount;
    
    private String status; // PENDING_PAYMENT, PAID, SHIPPED, COMPLETED, CANCELLED
    
    private String paymentType;
    
    private Long addressId;
    
    private String remark;
    
    private Date createdAt;
    
    private Date updatedAt;
    
    private Date paymentTime;
    
    private Date deliveryTime;
    
    private Date receiveTime;
    
    private Date cancelTime;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
