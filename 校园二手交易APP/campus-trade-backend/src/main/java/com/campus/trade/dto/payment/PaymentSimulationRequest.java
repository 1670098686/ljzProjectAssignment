package com.campus.trade.dto.payment;

import jakarta.validation.constraints.NotNull;

public class PaymentSimulationRequest {

    @NotNull
    private PaymentSimulationResult result;

    public PaymentSimulationResult getResult() {
        return result;
    }

    public void setResult(PaymentSimulationResult result) {
        this.result = result;
    }
}
