package com.campus.trade.dto.user;

public class NotificationSettingRequest {

    private Boolean notifyChat;
    private Boolean notifyOrders;
    private Boolean notifySystem;
    private Boolean notifyMarketing;

    public Boolean getNotifyChat() {
        return notifyChat;
    }

    public void setNotifyChat(Boolean notifyChat) {
        this.notifyChat = notifyChat;
    }

    public Boolean getNotifyOrders() {
        return notifyOrders;
    }

    public void setNotifyOrders(Boolean notifyOrders) {
        this.notifyOrders = notifyOrders;
    }

    public Boolean getNotifySystem() {
        return notifySystem;
    }

    public void setNotifySystem(Boolean notifySystem) {
        this.notifySystem = notifySystem;
    }

    public Boolean getNotifyMarketing() {
        return notifyMarketing;
    }

    public void setNotifyMarketing(Boolean notifyMarketing) {
        this.notifyMarketing = notifyMarketing;
    }
}
