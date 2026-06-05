package com.campus.trade.websocket;

import com.campus.trade.dto.message.NotificationSocketEvent;
import com.campus.trade.dto.message.SystemNotificationResponse;
import com.campus.trade.model.entity.SystemNotification;
import com.campus.trade.util.NotificationMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationSocketPublisher {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishCreated(SystemNotification notification) {
        SystemNotificationResponse payload = NotificationMapper.toResponse(notification);
        NotificationSocketEvent event = NotificationSocketEvent.newNotification(payload);
        send(notification, event);
    }

    public void publishRead(SystemNotification notification) {
        if (!notification.isRead()) {
            return;
        }
        NotificationSocketEvent event = NotificationSocketEvent.notificationRead(
                notification.getId(),
                notification.getReadTime());
        send(notification, event);
    }

    private void send(SystemNotification notification, NotificationSocketEvent event) {
        if (notification == null || notification.getUser() == null) {
            return;
        }
        String username = notification.getUser().getUsername();
        if (!StringUtils.hasText(username)) {
            return;
        }
        messagingTemplate.convertAndSendToUser(username, DESTINATION, event);
    }
}
