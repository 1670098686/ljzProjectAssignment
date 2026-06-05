package com.campus.trade.dto.user;

public class NotificationSettingResponse {

    private boolean notifyChat;
    private boolean notifyOrders;
    private boolean notifySystem;
    private boolean notifyMarketing;

    public boolean isNotifyChat() {
        return notifyChat;
    }

    public void setNotifyChat(boolean notifyChat) {
        this.notifyChat = notifyChat;
    }

    public boolean isNotifyOrders() {
        return notifyOrders;
    }

    public void setNotifyOrders(boolean notifyOrders) {
        this.notifyOrders = notifyOrders;
    }

    public boolean isNotifySystem() {
        return notifySystem;
    }

    public void setNotifySystem(boolean notifySystem) {
        this.notifySystem = notifySystem;
    }

    public boolean isNotifyMarketing() {
        return notifyMarketing;
    }

    public void setNotifyMarketing(boolean notifyMarketing) {
        this.notifyMarketing = notifyMarketing;
    }
}
