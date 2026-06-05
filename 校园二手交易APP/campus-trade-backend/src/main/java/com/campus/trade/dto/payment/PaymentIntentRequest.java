package com.campus.trade.dto.payment;

import jakarta.validation.constraints.NotBlank;

public class PaymentIntentRequest {

    @NotBlank
    private String method;

    private String channel;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
