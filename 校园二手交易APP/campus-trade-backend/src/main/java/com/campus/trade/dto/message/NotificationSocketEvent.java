package com.campus.trade.dto.message;

import java.time.LocalDateTime;

public class NotificationSocketEvent {

    private NotificationEventType type;
    private SystemNotificationResponse notification;
    private Long notificationId;
    private LocalDateTime readTime;

    public static NotificationSocketEvent newNotification(SystemNotificationResponse notification) {
        NotificationSocketEvent event = new NotificationSocketEvent();
        event.setType(NotificationEventType.NEW_NOTIFICATION);
        event.setNotification(notification);
        event.setNotificationId(notification != null ? notification.getId() : null);
        return event;
    }

    public static NotificationSocketEvent notificationRead(Long notificationId, LocalDateTime readTime) {
        NotificationSocketEvent event = new NotificationSocketEvent();
        event.setType(NotificationEventType.NOTIFICATION_READ);
        event.setNotificationId(notificationId);
        event.setReadTime(readTime);
        return event;
    }

    public NotificationEventType getType() {
        return type;
    }

    public void setType(NotificationEventType type) {
        this.type = type;
    }

    public SystemNotificationResponse getNotification() {
        return notification;
    }

    public void setNotification(SystemNotificationResponse notification) {
        this.notification = notification;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public enum NotificationEventType {
        NEW_NOTIFICATION,
        NOTIFICATION_READ
    }
}
