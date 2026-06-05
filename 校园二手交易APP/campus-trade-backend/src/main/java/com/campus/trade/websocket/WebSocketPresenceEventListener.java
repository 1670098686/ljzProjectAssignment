package com.campus.trade.websocket;

import com.campus.trade.service.PresenceService;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPresenceEventListener.class);

    private final PresenceService presenceService;

    public WebSocketPresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.debug("Session connected without principal, skip presence tracking");
            return;
        }
        presenceService.handleSessionConnected(principal.getName(), accessor.getSessionId());
    }

    @EventListener
    public void handleDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        presenceService.handleSessionDisconnected(accessor.getSessionId());
    }
}
