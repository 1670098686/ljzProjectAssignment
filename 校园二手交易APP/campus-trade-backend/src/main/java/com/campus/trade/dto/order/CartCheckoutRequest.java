package com.campus.trade.dto.order;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CartCheckoutRequest {

    @NotEmpty
    private List<Long> cartItemIds;

    private String note;

    private String shippingAddress;

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
