package com.campus.trade.websocket;

import com.campus.trade.dto.presence.PresenceSocketEvent;
import com.campus.trade.dto.presence.PresenceStatusResponse;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PresenceSocketPublisher {

    private static final String BROADCAST_DESTINATION = "/topic/presence";
    private static final String PERSONAL_DESTINATION = "/queue/presence";

    private final SimpMessagingTemplate messagingTemplate;

    public PresenceSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(PresenceSocketEvent event) {
        if (event == null) {
            return;
        }
        messagingTemplate.convertAndSend(BROADCAST_DESTINATION, event);
    }

    public void sendSnapshot(String username, List<PresenceStatusResponse> statuses) {
        if (!StringUtils.hasText(username) || statuses == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(username, PERSONAL_DESTINATION, PresenceSocketEvent.snapshot(statuses));
    }
}
