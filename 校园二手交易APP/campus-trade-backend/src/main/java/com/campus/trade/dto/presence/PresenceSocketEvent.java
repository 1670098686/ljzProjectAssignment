package com.campus.trade.dto.presence;

import java.util.ArrayList;
import java.util.List;

public class PresenceSocketEvent {

    private PresenceSocketEventType type;
    private PresenceStatusResponse user;
    private List<PresenceStatusResponse> snapshot = new ArrayList<>();

    public PresenceSocketEventType getType() {
        return type;
    }

    public void setType(PresenceSocketEventType type) {
        this.type = type;
    }

    public PresenceStatusResponse getUser() {
        return user;
    }

    public void setUser(PresenceStatusResponse user) {
        this.user = user;
    }

    public List<PresenceStatusResponse> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(List<PresenceStatusResponse> snapshot) {
        this.snapshot = snapshot;
    }

    public static PresenceSocketEvent online(PresenceStatusResponse user) {
        PresenceSocketEvent event = new PresenceSocketEvent();
        event.setType(PresenceSocketEventType.ONLINE);
        event.setUser(user);
        return event;
    }

    public static PresenceSocketEvent offline(PresenceStatusResponse user) {
        PresenceSocketEvent event = new PresenceSocketEvent();
        event.setType(PresenceSocketEventType.OFFLINE);
        event.setUser(user);
        return event;
    }

    public static PresenceSocketEvent heartbeat(PresenceStatusResponse user) {
        PresenceSocketEvent event = new PresenceSocketEvent();
        event.setType(PresenceSocketEventType.HEARTBEAT);
        event.setUser(user);
        return event;
    }

    public static PresenceSocketEvent snapshot(List<PresenceStatusResponse> snapshot) {
        PresenceSocketEvent event = new PresenceSocketEvent();
        event.setType(PresenceSocketEventType.SNAPSHOT);
        event.setSnapshot(new ArrayList<>(snapshot));
        return event;
    }
}
