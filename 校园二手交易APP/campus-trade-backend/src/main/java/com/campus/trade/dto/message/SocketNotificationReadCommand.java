package com.campus.trade.dto.message;

import jakarta.validation.constraints.NotNull;

/**
 * Command payload for marking a notification as read via WebSocket.
 */
public class SocketNotificationReadCommand {

    @NotNull
    private Long notificationId;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
