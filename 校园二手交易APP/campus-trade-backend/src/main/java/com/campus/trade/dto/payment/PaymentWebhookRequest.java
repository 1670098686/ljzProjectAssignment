package com.campus.trade.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class PaymentWebhookRequest {

    @NotBlank
    private String referenceNo;

    @NotNull
    private PaymentWebhookStatus status;

    @NotBlank
    private String signature;

    private Map<String, Object> payload;

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public PaymentWebhookStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentWebhookStatus status) {
        this.status = status;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
