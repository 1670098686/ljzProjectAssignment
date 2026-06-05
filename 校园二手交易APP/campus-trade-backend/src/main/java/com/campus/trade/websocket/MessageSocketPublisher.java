package com.campus.trade.websocket;

import com.campus.trade.dto.message.MessageSocketEvent;
import com.campus.trade.model.entity.Message;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.RelatedType;
import com.campus.trade.util.MessageMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MessageSocketPublisher {

    private static final String DESTINATION = "/queue/messages";

    private final SimpMessagingTemplate messagingTemplate;

    public MessageSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishNewMessage(Message message) {
        MessageSocketEvent event = MessageSocketEvent.newMessage(MessageMapper.toResponse(message));
        sendToUser(message.getReceiver().getUsername(), event);
        sendToUser(message.getSender().getUsername(), event);
    }

    public void publishMessagesRead(User actor, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Map<ReadReceiptKey, List<Long>> grouped = messages.stream()
                .collect(Collectors.groupingBy(
                        message -> new ReadReceiptKey(
                                message.getSender().getUsername(),
                                message.getRelatedType(),
                                message.getRelatedId()),
                        Collectors.mapping(Message::getId, Collectors.toList())));
        grouped.forEach((key, ids) -> {
            MessageSocketEvent event = MessageSocketEvent.messagesRead(
                    actor.getId(),
                    key.relatedType(),
                    key.relatedId(),
                    ids);
            sendToUser(key.username(), event);
        });
    }

    private void sendToUser(String username, MessageSocketEvent payload) {
        messagingTemplate.convertAndSendToUser(username, DESTINATION, payload);
    }

    private record ReadReceiptKey(String username, RelatedType relatedType, Long relatedId) {
    }
}
