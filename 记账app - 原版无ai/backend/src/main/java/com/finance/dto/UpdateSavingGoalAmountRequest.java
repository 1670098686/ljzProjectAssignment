package com.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateSavingGoalAmountRequest {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal currentAmount;

    public UpdateSavingGoalAmountRequest() {
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }
}
