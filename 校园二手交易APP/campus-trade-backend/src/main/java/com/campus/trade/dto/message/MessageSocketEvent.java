package com.campus.trade.dto.message;

import com.campus.trade.model.enums.RelatedType;

import java.util.ArrayList;
import java.util.List;

public class MessageSocketEvent {

    private MessageSocketEventType type;
    private MessageResponse message;
    private Long actorId;
    private RelatedType relatedType;
    private Long relatedId;
    private List<Long> messageIds = new ArrayList<>();

    public MessageSocketEventType getType() {
        return type;
    }

    public void setType(MessageSocketEventType type) {
        this.type = type;
    }

    public MessageResponse getMessage() {
        return message;
    }

    public void setMessage(MessageResponse message) {
        this.message = message;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public RelatedType getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(RelatedType relatedType) {
        this.relatedType = relatedType;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }

    public static MessageSocketEvent newMessage(MessageResponse response) {
        MessageSocketEvent event = new MessageSocketEvent();
        event.setType(MessageSocketEventType.MESSAGE_CREATED);
        event.setMessage(response);
        if (response != null) {
            event.setRelatedType(response.getRelatedType());
            event.setRelatedId(response.getRelatedId());
        }
        return event;
    }

    public static MessageSocketEvent messagesRead(Long actorId,
                                                  RelatedType relatedType,
                                                  Long relatedId,
                                                  List<Long> ids) {
        MessageSocketEvent event = new MessageSocketEvent();
        event.setType(MessageSocketEventType.MESSAGES_READ);
        event.setActorId(actorId);
        event.setRelatedType(relatedType);
        event.setRelatedId(relatedId);
        event.setMessageIds(new ArrayList<>(ids));
        return event;
    }
}
