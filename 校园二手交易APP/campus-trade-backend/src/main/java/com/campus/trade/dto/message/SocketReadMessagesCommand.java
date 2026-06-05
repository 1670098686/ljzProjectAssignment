package com.campus.trade.dto.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Command payload for marking specific messages as read via WebSocket.
 */
public class SocketReadMessagesCommand {

    @NotEmpty
    @Size(max = 50)
    private List<Long> messageIds;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }

    public ReadMessagesRequest toReadMessagesRequest() {
        ReadMessagesRequest request = new ReadMessagesRequest();
        request.setMessageIds(this.messageIds);
        return request;
    }
}
