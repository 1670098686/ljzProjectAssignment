package com.campus.trade.util;

import com.campus.trade.dto.message.SystemNotificationResponse;
import com.campus.trade.model.entity.SystemNotification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static SystemNotificationResponse toResponse(SystemNotification notification) {
        if (notification == null) {
            return null;
        }
        SystemNotificationResponse response = new SystemNotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRead(notification.isRead());
        response.setReadTime(notification.getReadTime());
        response.setCreateTime(notification.getCreateTime());
        return response;
    }
}
