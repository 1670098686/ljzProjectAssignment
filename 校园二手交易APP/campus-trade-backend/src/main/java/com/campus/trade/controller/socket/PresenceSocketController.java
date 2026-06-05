package com.campus.trade.controller.socket;

import com.campus.trade.service.PresenceService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class PresenceSocketController {

    private final PresenceService presenceService;

    public PresenceSocketController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/presence/ping")
    public void ping(Principal principal) {
        if (principal == null) {
            return;
        }
        presenceService.handleHeartbeat(principal.getName());
    }

    @MessageMapping("/presence/list")
    public void snapshot(Principal principal) {
        if (principal == null) {
            return;
        }
        presenceService.sendSnapshot(principal.getName());
    }
}
