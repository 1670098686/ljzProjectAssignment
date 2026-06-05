package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.message.ConversationRequest;
import com.campus.trade.dto.message.ConversationSummaryResponse;
import com.campus.trade.dto.message.ReportMessageRequest;
import com.campus.trade.dto.message.SendMessageRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.ConversationSetting;
import com.campus.trade.model.entity.Message;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.RelatedType;
import com.campus.trade.repository.ConversationSettingRepository;
import com.campus.trade.repository.MessageReportRepository;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.websocket.MessageSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MessageReportRepository messageReportRepository;

    @Mock
    private ConversationSettingRepository conversationSettingRepository;

    @Mock
    private MessageSocketPublisher messageSocketPublisher;

    @InjectMocks
    private MessageService messageService;

    private User buyer;
    private User seller;

    @BeforeEach
    void setUp() {
        buyer = user(1L, "buyer");
        seller = user(2L, "seller");
    }

    @Test
    void listConversations_returnsSummariesWithUnreadCount() {
        Message latest = message(100L, buyer, seller, true, LocalDateTime.now());
        Message unread = message(101L, seller, buyer, false, LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(conversationSettingRepository.findByOwnerId(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.findBySenderIdOrReceiverIdOrderByCreateTimeDesc(1L, 1L))
                .thenReturn(List.of(latest, unread));

        PaginatedResponse<ConversationSummaryResponse> response = messageService.listConversations("buyer", 1, 10);

        assertEquals(1, response.getItems().size());
        ConversationSummaryResponse summary = response.getItems().get(0);
        assertEquals(seller.getId(), summary.getPartner().getId());
        assertEquals(latest.getId(), summary.getLastMessage().getId());
        assertEquals(1, summary.getUnreadCount());
        assertNull(summary.getRelatedType());
        assertNull(summary.getRelatedId());
        assertEquals(1, response.getMeta().getTotal());
    }

    @Test
    void listConversations_respectsDeletedConversations() {
        ConversationSetting hidden = new ConversationSetting();
        hidden.setOwner(buyer);
        hidden.setPartner(seller);
        hidden.setDeletedAt(LocalDateTime.now());

        Message oldMessage = message(200L, seller, buyer, false, LocalDateTime.now().minusDays(1));

        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(conversationSettingRepository.findByOwnerId(1L)).thenReturn(List.of(hidden));
        when(messageRepository.findBySenderIdOrReceiverIdOrderByCreateTimeDesc(1L, 1L))
                .thenReturn(List.of(oldMessage));

        PaginatedResponse<ConversationSummaryResponse> response = messageService.listConversations("buyer", 1, 10);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getMeta().getTotal());
    }

    @Test
    void listConversations_splitsContextsByRelatedInfo() {
        Message productMessage = message(210L, seller, buyer, false, LocalDateTime.now());
        productMessage.setRelatedType(RelatedType.PRODUCT);
        productMessage.setRelatedId(55L);

        Message orderMessage = message(211L, seller, buyer, true, LocalDateTime.now().minusMinutes(1));
        orderMessage.setRelatedType(RelatedType.ORDER);
        orderMessage.setRelatedId(77L);

        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(conversationSettingRepository.findByOwnerId(1L)).thenReturn(Collections.emptyList());
        when(messageRepository.findBySenderIdOrReceiverIdOrderByCreateTimeDesc(1L, 1L))
                .thenReturn(List.of(productMessage, orderMessage));

        PaginatedResponse<ConversationSummaryResponse> response = messageService.listConversations("buyer", 1, 10);

        assertEquals(2, response.getItems().size());
        ConversationSummaryResponse first = response.getItems().get(0);
        assertEquals(RelatedType.PRODUCT, first.getRelatedType());
        assertEquals(55L, first.getRelatedId());
        ConversationSummaryResponse second = response.getItems().get(1);
        assertEquals(RelatedType.ORDER, second.getRelatedType());
        assertEquals(77L, second.getRelatedId());
    }

    @Test
    void reportMessage_requiresParticipant() {
        User stranger = user(3L, "other");
        Message message = message(300L, buyer, seller, true, LocalDateTime.now());
        ReportMessageRequest request = new ReportMessageRequest();
        request.setMessageId(300L);
        request.setReason("spam");

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(stranger));
        when(messageRepository.findById(300L)).thenReturn(Optional.of(message));

        assertThrows(BusinessException.class, () -> messageService.reportMessage("other", request));
    }

    @Test
    void sendMessage_rejectsMultipleContexts() {
        SendMessageRequest request = new SendMessageRequest();
        request.setToUserId(2L);
        request.setProductId(11L);
        request.setOrderId(22L);
        request.setContent("hi");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> messageService.sendMessage("buyer", request));

        assertEquals(ErrorCode.BUSINESS_ERROR, exception.getErrorCode());
    }

    @Test
    void markConversationRead_filtersByProductContext() {
        ConversationRequest request = new ConversationRequest();
        request.setToUserId(2L);
        request.setProductId(99L);

        Message unread = message(400L, seller, buyer, false, LocalDateTime.now());

        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(messageRepository.findAll(Mockito.<Specification<Message>>any())).thenReturn(List.of(unread));

        messageService.markConversationRead("buyer", request);

        assertTrue(unread.isRead());
        assertNotNull(unread.getReadTime());
        verify(messageRepository).findAll(Mockito.<Specification<Message>>any());
    }

    @Test
    void deleteConversation_createsScopedSetting() {
        ConversationRequest request = new ConversationRequest();
        request.setToUserId(2L);
        request.setProductId(55L);

        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(conversationSettingRepository.findByOwnerIdAndPartnerIdAndRelatedTypeAndRelatedId(1L, 2L, RelatedType.PRODUCT, 55L))
                .thenReturn(Optional.empty());

        messageService.deleteConversation("buyer", request);

        ArgumentCaptor<ConversationSetting> captor = ArgumentCaptor.forClass(ConversationSetting.class);
        verify(conversationSettingRepository).save(captor.capture());
        ConversationSetting saved = captor.getValue();
        assertEquals(RelatedType.PRODUCT, saved.getRelatedType());
        assertEquals(55L, saved.getRelatedId());
        assertNotNull(saved.getDeletedAt());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        return user;
    }

    private Message message(Long id, User sender, User receiver, boolean read, LocalDateTime createTime) {
        Message message = new Message();
        message.setId(id);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("hello");
        message.setRead(read);
        message.setCreateTime(createTime);
        return message;
    }
}
