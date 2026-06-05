package com.campus.trade.dto.product;

import jakarta.validation.constraints.NotNull;

public class ProductReviewRequest {

    @NotNull
    private Boolean approved;

    private String reason;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
