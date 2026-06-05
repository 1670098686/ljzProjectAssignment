package com.campus.trade.websocket;

import com.campus.trade.dto.message.MessageSocketEvent;
import com.campus.trade.dto.message.MessageSocketEventType;
import com.campus.trade.model.entity.Message;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.RelatedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MessageSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageSocketPublisher publisher;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
    }

    @Test
    void publishNewMessage_broadcastsToParticipants() {
        Message message = message(10L, alice, bob);

        publisher.publishNewMessage(message);

        ArgumentCaptor<MessageSocketEvent> receiverCaptor = ArgumentCaptor.forClass(MessageSocketEvent.class);
        verify(messagingTemplate).convertAndSendToUser(eq("bob"), eq("/queue/messages"), receiverCaptor.capture());
        MessageSocketEvent event = receiverCaptor.getValue();
        assertEquals(MessageSocketEventType.MESSAGE_CREATED, event.getType());
        assertNotNull(event.getMessage());
        assertEquals(10L, event.getMessage().getId());

        verify(messagingTemplate).convertAndSendToUser(eq("alice"), eq("/queue/messages"), any(MessageSocketEvent.class));
    }

    @Test
    void publishMessagesRead_groupsByConversationContext() {
        Message first = message(11L, alice, bob);
        first.setRelatedType(RelatedType.PRODUCT);
        first.setRelatedId(99L);
        Message second = message(12L, alice, bob);
        second.setRelatedType(RelatedType.PRODUCT);
        second.setRelatedId(99L);

        publisher.publishMessagesRead(bob, List.of(first, second));

        ArgumentCaptor<MessageSocketEvent> captor = ArgumentCaptor.forClass(MessageSocketEvent.class);
        verify(messagingTemplate).convertAndSendToUser(eq("alice"), eq("/queue/messages"), captor.capture());
        MessageSocketEvent event = captor.getValue();
        assertEquals(MessageSocketEventType.MESSAGES_READ, event.getType());
        assertEquals(bob.getId(), event.getActorId());
        assertEquals(RelatedType.PRODUCT, event.getRelatedType());
        assertEquals(99L, event.getRelatedId());
        assertEquals(List.of(11L, 12L), event.getMessageIds());
        verifyNoMoreInteractions(messagingTemplate);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        return user;
    }

    private Message message(Long id, User sender, User receiver) {
        Message message = new Message();
        message.setId(id);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("hello");
        return message;
    }
}
