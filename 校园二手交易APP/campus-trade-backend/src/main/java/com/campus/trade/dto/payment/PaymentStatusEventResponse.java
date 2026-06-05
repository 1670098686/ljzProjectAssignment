package com.campus.trade.dto.payment;

import com.campus.trade.model.enums.PaymentStatus;

import java.time.LocalDateTime;

public class PaymentStatusEventResponse {

    private PaymentStatus status;
    private LocalDateTime timestamp;

    public PaymentStatusEventResponse() {
    }

    public PaymentStatusEventResponse(PaymentStatus status, LocalDateTime timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
