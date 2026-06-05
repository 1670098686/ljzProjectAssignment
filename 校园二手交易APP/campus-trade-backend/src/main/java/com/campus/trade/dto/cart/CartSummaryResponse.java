package com.campus.trade.dto.cart;

import java.math.BigDecimal;

public class CartSummaryResponse {

    private int totalQuantity;
    private int uniqueProducts;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getUniqueProducts() {
        return uniqueProducts;
    }

    public void setUniqueProducts(int uniqueProducts) {
        this.uniqueProducts = uniqueProducts;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
