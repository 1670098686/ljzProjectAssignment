package com.campus.trade.dto.cart;

public class CartCountResponse {

    private long totalItems;

    public CartCountResponse() {
    }

    public CartCountResponse(long totalItems) {
        this.totalItems = totalItems;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }
}
