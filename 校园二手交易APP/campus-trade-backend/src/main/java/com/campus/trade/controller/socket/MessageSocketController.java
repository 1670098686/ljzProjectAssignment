package com.campus.trade.controller.socket;

import com.campus.trade.dto.message.SocketReadMessagesCommand;
import com.campus.trade.dto.message.SocketSendMessageCommand;
import com.campus.trade.service.MessageService;
import com.campus.trade.service.PresenceService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class MessageSocketController {

    private final MessageService messageService;
    private final PresenceService presenceService;

    public MessageSocketController(MessageService messageService, PresenceService presenceService) {
        this.messageService = messageService;
        this.presenceService = presenceService;
    }

    @MessageMapping("/messages/send")
    public void send(@Valid @Payload SocketSendMessageCommand command, Principal principal) {
        if (principal == null) {
            return;
        }
        messageService.sendMessage(principal.getName(), command.toSendMessageRequest());
        presenceService.handleHeartbeat(principal.getName());
    }

    @MessageMapping("/messages/read")
    public void markRead(@Valid @Payload SocketReadMessagesCommand command, Principal principal) {
        if (principal == null) {
            return;
        }
        messageService.markMessagesRead(principal.getName(), command.toReadMessagesRequest());
        presenceService.handleHeartbeat(principal.getName());
    }
}
