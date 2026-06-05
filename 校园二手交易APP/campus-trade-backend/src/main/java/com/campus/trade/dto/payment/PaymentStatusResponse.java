package com.campus.trade.dto.payment;

import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.RefundStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PaymentStatusResponse {

    private Long orderId;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private LocalDateTime paymentTime;
    private LocalDateTime paymentExpireTime;
    private Map<String, Object> paymentMetadata;
    private RefundStatus refundStatus;
    private LocalDateTime refundTime;
    private PaymentIntentResponse latestIntent;
    private List<PaymentRecordResponse> history;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public LocalDateTime getPaymentExpireTime() {
        return paymentExpireTime;
    }

    public void setPaymentExpireTime(LocalDateTime paymentExpireTime) {
        this.paymentExpireTime = paymentExpireTime;
    }

    public Map<String, Object> getPaymentMetadata() {
        return paymentMetadata;
    }

    public void setPaymentMetadata(Map<String, Object> paymentMetadata) {
        this.paymentMetadata = paymentMetadata;
    }

    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(RefundStatus refundStatus) {
        this.refundStatus = refundStatus;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }

    public PaymentIntentResponse getLatestIntent() {
        return latestIntent;
    }

    public void setLatestIntent(PaymentIntentResponse latestIntent) {
        this.latestIntent = latestIntent;
    }

    public List<PaymentRecordResponse> getHistory() {
        return history;
    }

    public void setHistory(List<PaymentRecordResponse> history) {
        this.history = history;
    }
}
