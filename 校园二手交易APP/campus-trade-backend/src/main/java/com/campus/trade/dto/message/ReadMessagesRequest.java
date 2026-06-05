package com.campus.trade.dto.message;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ReadMessagesRequest {

    @NotEmpty
    private List<Long> messageIds;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }
}
