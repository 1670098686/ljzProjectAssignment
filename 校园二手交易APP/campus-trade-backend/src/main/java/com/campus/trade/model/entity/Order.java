package com.campus.trade.model.entity;

import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_INITIATED;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 64)
    private String paymentReference;

    @Column(name = "payment_expire_time")
    private LocalDateTime paymentExpireTime;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Lob
    @Column(name = "payment_metadata")
    private String paymentMetadata;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "receive_time")
    private LocalDateTime receiveTime;

    @Lob
    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "buyer_note", length = 200)
    private String buyerNote;

    @Column(name = "seller_note", length = 200)
    private String sellerNote;

    @Column(name = "buyer_rating")
    private Integer buyerRating;

    @Column(name = "buyer_comment", length = 300)
    private String buyerComment;

    @Column(name = "seller_rating")
    private Integer sellerRating;

    @Column(name = "seller_comment", length = 300)
    private String sellerComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "refund_time")
    private LocalDateTime refundTime;
}
