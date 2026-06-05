package com.campus.trade.dto.payment;

import com.campus.trade.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PaymentRecordResponse {

    private Long paymentId;
    private String referenceNo;
    private PaymentStatus status;
    private String method;
    private String channel;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime expiresAt;
    private List<PaymentStatusEventResponse> statusTimeline;
    private Map<String, Object> requestPayload;
    private Map<String, Object> responsePayload;
    private String webhookStatus;
    private LocalDateTime callbackAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<PaymentStatusEventResponse> getStatusTimeline() {
        return statusTimeline;
    }

    public void setStatusTimeline(List<PaymentStatusEventResponse> statusTimeline) {
        this.statusTimeline = statusTimeline;
    }

    public Map<String, Object> getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(Map<String, Object> requestPayload) {
        this.requestPayload = requestPayload;
    }

    public Map<String, Object> getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(Map<String, Object> responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getWebhookStatus() {
        return webhookStatus;
    }

    public void setWebhookStatus(String webhookStatus) {
        this.webhookStatus = webhookStatus;
    }

    public LocalDateTime getCallbackAt() {
        return callbackAt;
    }

    public void setCallbackAt(LocalDateTime callbackAt) {
        this.callbackAt = callbackAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
