package com.campus.trade.controller.socket;

import com.campus.trade.dto.message.SocketNotificationReadCommand;
import com.campus.trade.service.NotificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class NotificationSocketController {

    private final NotificationService notificationService;

    public NotificationSocketController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/notifications/read")
    public void markNotificationRead(@Valid @Payload SocketNotificationReadCommand command, Principal principal) {
        if (principal == null) {
            return;
        }
        notificationService.markRead(principal.getName(), command.getNotificationId());
    }
}
