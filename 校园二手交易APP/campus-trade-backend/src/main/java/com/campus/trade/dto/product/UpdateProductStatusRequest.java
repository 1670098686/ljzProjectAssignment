package com.campus.trade.dto.product;

import com.campus.trade.model.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateProductStatusRequest {

    @NotNull
    private ProductStatus status;

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }
}
