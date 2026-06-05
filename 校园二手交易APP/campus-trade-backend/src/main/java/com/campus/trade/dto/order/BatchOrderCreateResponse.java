package com.campus.trade.dto.order;

import java.util.ArrayList;
import java.util.List;

public class BatchOrderCreateResponse {

    private List<OrderResponse> orders = new ArrayList<>();

    private List<FailedItem> failed = new ArrayList<>();

    public List<OrderResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderResponse> orders) {
        this.orders = orders;
    }

    public List<FailedItem> getFailed() {
        return failed;
    }

    public void setFailed(List<FailedItem> failed) {
        this.failed = failed;
    }

    public static class FailedItem {
        private Long cartItemId;
        private String reason;

        public FailedItem() {
        }

        public FailedItem(Long cartItemId, String reason) {
            this.cartItemId = cartItemId;
            this.reason = reason;
        }

        public Long getCartItemId() {
            return cartItemId;
        }

        public void setCartItemId(Long cartItemId) {
            this.cartItemId = cartItemId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
