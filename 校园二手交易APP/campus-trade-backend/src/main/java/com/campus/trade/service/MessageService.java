package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.message.ConversationRequest;
import com.campus.trade.dto.message.ConversationSummaryResponse;
import com.campus.trade.dto.message.MessageReportResponse;
import com.campus.trade.dto.message.MessageResponse;
import com.campus.trade.dto.message.ReadMessagesRequest;
import com.campus.trade.dto.message.ReportMessageRequest;
import com.campus.trade.dto.message.ResolveReportRequest;
import com.campus.trade.dto.message.SendMessageRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.ConversationSetting;
import com.campus.trade.model.entity.Message;
import com.campus.trade.model.entity.MessageReport;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.MessageType;
import com.campus.trade.model.enums.RelatedType;
import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.repository.ConversationSettingRepository;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.MessageReportRepository;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.MessageMapper;
import com.campus.trade.util.UserMapper;
import com.campus.trade.websocket.MessageSocketPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

        private final MessageRepository messageRepository;
        private final UserRepository userRepository;
        private final ProductRepository productRepository;
        private final OrderRepository orderRepository;
        private final MessageReportRepository messageReportRepository;
        private final ConversationSettingRepository conversationSettingRepository;
        private final MessageSocketPublisher messageSocketPublisher;

        public MessageService(MessageRepository messageRepository,
                                                  UserRepository userRepository,
                                                  ProductRepository productRepository,
                                                  OrderRepository orderRepository,
                                                  MessageReportRepository messageReportRepository,
                                                  ConversationSettingRepository conversationSettingRepository,
                                                  MessageSocketPublisher messageSocketPublisher) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
                this.messageReportRepository = messageReportRepository;
                this.conversationSettingRepository = conversationSettingRepository;
                this.messageSocketPublisher = messageSocketPublisher;
    }

    @Transactional
    public MessageResponse sendMessage(String username, SendMessageRequest request) {
        validateConversationContext(request.getProductId(), request.getOrderId());
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

                MessageType messageType = request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT;
                String content = request.getContent() != null ? request.getContent() : "";
                String attachmentUrl = request.getAttachmentUrl();

                if (messageType == MessageType.TEXT) {
                        if (content.trim().isEmpty()) {
                                throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "消息内容不能为空");
                        }
                } else if (messageType == MessageType.IMAGE || messageType == MessageType.AUDIO) {
                        if (attachmentUrl == null || attachmentUrl.trim().isEmpty()) {
                                throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "附件地址不能为空");
                        }
                }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
                message.setContent(content);
                message.setMessageType(messageType);
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            message.setRelatedType(RelatedType.PRODUCT);
            message.setRelatedId(product.getId());
        }
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            message.setRelatedType(RelatedType.ORDER);
            message.setRelatedId(order.getId());
        }
                message.setAttachmentUrl(attachmentUrl);
                messageRepository.save(message);
                messageSocketPublisher.publishNewMessage(message);
        return MessageMapper.toResponse(message);
    }

    /**
     * 获取用户的对话列表
     *
     * @param username 当前用户名
     * @param page     页码
     * @param size     每页数量
     * @return 分页的对话列表响应
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ConversationSummaryResponse> listConversations(String username, int page, int size) {
        // 1. 获取当前用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 2. 验证分页参数
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 20); // 确保size不小于20，避免频繁查询
        
        // 3. 获取已隐藏的对话设置
        Map<ConversationContext, LocalDateTime> hiddenMap = conversationSettingRepository.findByOwnerId(user.getId()).stream()
                .filter(setting -> setting.getDeletedAt() != null)
                .collect(Collectors.toMap(this::toContextKey, ConversationSetting::getDeletedAt, (a, b) -> a, LinkedHashMap::new));
        
        // 4. 获取用户的所有消息，只获取最新的100条，足够构建对话列表
        // 这里优化为只查询最新的消息，而不是所有消息
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreateTimeDesc(user.getId(), user.getId())
                .stream()
                .limit(100) // 只取最新的100条消息，足够构建对话列表
                .collect(Collectors.toList());
        
        // 5. 构建对话映射，使用LinkedHashMap保持插入顺序
        Map<ConversationContext, ConversationAccumulator> aggregates = new LinkedHashMap<>();
        
        for (Message message : messages) {
            try {
                // 确保发送者和接收者被加载
                User sender = message.getSender();
                User receiver = message.getReceiver();
                
                // 访问id以确保实体被加载
                if (sender == null || receiver == null) {
                    continue;
                }
                
                // 强制加载关联实体
                sender.getId();
                receiver.getId();
                
                // 6. 构建对话上下文
                ConversationContext context = ConversationContext.fromMessage(user.getId(), message);
                LocalDateTime deletedAt = resolveDeletionTimestamp(hiddenMap, context);
                
                // 7. 跳过已删除的对话
                if (deletedAt != null && message.getCreateTime() != null && !message.getCreateTime().isAfter(deletedAt)) {
                    continue;
                }
                
                // 8. 计算对话对方
                User partner = sender.getId().equals(user.getId()) ? receiver : sender;
                
                // 9. 获取或创建对话累加器
                ConversationAccumulator accumulator = aggregates.computeIfAbsent(context, 
                    key -> new ConversationAccumulator(partner));
                
                // 10. 更新最新消息
                if (accumulator.getLastMessage() == null) {
                    accumulator.setLastMessage(message);
                }
                
                // 11. 更新未读消息数
                if (!message.isRead() && receiver.getId().equals(user.getId())) {
                    accumulator.incrementUnread();
                }
            } catch (Exception e) {
                // 跳过有问题的消息，继续处理其他消息
                continue;
            }
        }
        
        // 12. 转换为响应对象
        List<ConversationSummaryResponse> summaries = aggregates.entrySet().stream()
            .map(entry -> {
                ConversationContext context = entry.getKey();
                ConversationAccumulator acc = entry.getValue();
                
                ConversationSummaryResponse response = new ConversationSummaryResponse();
                response.setPartner(UserMapper.toMaskedSummary(acc.getPartner()));
                response.setLastMessage(MessageMapper.toResponse(acc.getLastMessage()));
                response.setUnreadCount(acc.getUnreadCount());
                response.setRelatedType(context.getRelatedType());
                response.setRelatedId(context.getRelatedId());
                
                return response;
            })
            .collect(Collectors.toList());
        
        // 13. 计算分页
        long total = summaries.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, summaries.size());
        int toIndex = Math.min(fromIndex + safeSize, summaries.size());
        
        // 14. 处理空列表情况
        List<ConversationSummaryResponse> pageItems = fromIndex < summaries.size() 
            ? summaries.subList(fromIndex, toIndex) 
            : new ArrayList<>();
        
        return PaginatedResponse.of(pageItems, safePage, safeSize, total);
    }

    @Transactional
    public void markMessagesRead(String username, ReadMessagesRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                List<Message> updated = new ArrayList<>();
                request.getMessageIds().forEach(id -> {
            Message message = messageRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));
            if (!message.getReceiver().getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "无权操作消息");
            }
            message.setRead(true);
            message.setReadTime(LocalDateTime.now());
                        updated.add(message);
        });
                messageSocketPublisher.publishMessagesRead(user, updated);
    }

    @Transactional
    public PaginatedResponse<MessageResponse> conversationHistory(String username,
                                                                  Long toUserId,
                                                                  Long productId,
                                                                  Long orderId,
                                                                  int page,
                                                                  int size) {
        if (toUserId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "对话用户不能为空");
        }
        validateConversationContext(productId, orderId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User other = userRepository.findById(toUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "对话用户不存在"));
        Specification<Message> spec = Specification.<Message>where((root, query, cb) -> cb.or(
                cb.and(
                        cb.equal(root.get("sender").get("id"), user.getId()),
                        cb.equal(root.get("receiver").get("id"), toUserId)
                ),
                cb.and(
                        cb.equal(root.get("sender").get("id"), toUserId),
                        cb.equal(root.get("receiver").get("id"), user.getId())
                )
        ));
        if (productId != null) {
                        spec = spec.and((root, query, cb) -> cb.and(
                    cb.equal(root.get("relatedType"), RelatedType.PRODUCT),
                    cb.equal(root.get("relatedId"), productId)
            ));
        }
        if (orderId != null) {
                        spec = spec.and((root, query, cb) -> cb.and(
                    cb.equal(root.get("relatedType"), RelatedType.ORDER),
                    cb.equal(root.get("relatedId"), orderId)
            ));
        }
        LocalDateTime deletedAt = resolveConversationDeletion(user, other, productId, orderId);
        if (deletedAt != null) {
            LocalDateTime finalDeletedAt = deletedAt;
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("createTime"), finalDeletedAt));
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        Page<Message> result = messageRepository.findAll(spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime")));
        
        // 确保在事务内加载所有必要的关联实体
        List<Message> messages = result.getContent();
        for (Message message : messages) {
            // 访问发送者和接收者的id，确保实体被加载
            message.getSender().getId();
            message.getReceiver().getId();
        }
        
        return PaginatedResponse.of(messages.stream().map(MessageMapper::toResponse).collect(java.util.stream.Collectors.toList()), safePage, safeSize, result.getTotalElements());
    }

    @Transactional
    public void markConversationRead(String username, ConversationRequest request) {
        Long toUserId = request.getToUserId();
        if (toUserId == null) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "对话用户不能为空");
        }
        validateConversationContext(request.getProductId(), request.getOrderId());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User other = userRepository.findById(toUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "对话用户不存在"));
        Specification<Message> unreadSpec = buildUnreadConversationSpec(user.getId(), other.getId(), request.getProductId(), request.getOrderId());
        List<Message> unreadMessages = messageRepository.findAll(unreadSpec);
        LocalDateTime now = LocalDateTime.now();
                unreadMessages.forEach(message -> {
            message.setRead(true);
            message.setReadTime(now);
        });
                messageSocketPublisher.publishMessagesRead(user, unreadMessages);
    }

        @Transactional
        public void deleteConversation(String username, ConversationRequest request) {
                Long toUserId = request.getToUserId();
                if (toUserId == null) {
                        throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "对话用户不能为空");
                }
                validateConversationContext(request.getProductId(), request.getOrderId());
                User owner = userRepository.findByUsername(username)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                User partner = userRepository.findById(toUserId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "对话用户不存在"));
                ConversationSetting setting = getOrCreateSetting(owner, partner, request.getProductId(), request.getOrderId());
                setting.setDeletedAt(LocalDateTime.now());
                conversationSettingRepository.save(setting);
        }

        @Transactional
        public void reportMessage(String username, ReportMessageRequest request) {
                User reporter = userRepository.findByUsername(username)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                Message message = messageRepository.findById(request.getMessageId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "消息不存在"));
                if (!message.getSender().getId().equals(reporter.getId()) && !message.getReceiver().getId().equals(reporter.getId())) {
                        throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "无权举报该消息");
                }
                MessageReport report = new MessageReport();
                report.setMessage(message);
                report.setReporter(reporter);
                report.setReason(request.getReason());
                report.setEvidenceUrl(request.getEvidenceUrl());
                messageReportRepository.save(report);
        }

        public PaginatedResponse<MessageReportResponse> listReports(ReportStatus status, int page, int size) {
                Page<MessageReport> result;
                PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
                if (status == null) {
                        result = messageReportRepository.findAll(pageable);
                } else {
                        result = messageReportRepository.findByStatus(status, pageable);
                }
                return PaginatedResponse.of(result.map(this::toReportResponse).getContent(), page, size, result.getTotalElements());
        }

        @Transactional
        public void resolveReport(Long reportId, ResolveReportRequest request) {
                MessageReport report = messageReportRepository.findById(reportId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "举报不存在"));
                report.setStatus(request.getStatus());
                report.setResolution(request.getResolution());
                if (request.getStatus() == ReportStatus.RESOLVED || request.getStatus() == ReportStatus.REJECTED) {
                        report.setResolvedTime(LocalDateTime.now());
                } else {
                        report.setResolvedTime(null);
                }
        }

        private MessageReportResponse toReportResponse(MessageReport report) {
                MessageReportResponse response = new MessageReportResponse();
                response.setId(report.getId());
                response.setMessageId(report.getMessage().getId());
                response.setReporter(UserMapper.toMaskedSummary(report.getReporter()));
                response.setReason(report.getReason());
                response.setEvidenceUrl(report.getEvidenceUrl());
                response.setStatus(report.getStatus());
                response.setResolution(report.getResolution());
                response.setCreateTime(report.getCreateTime());
                response.setResolvedTime(report.getResolvedTime());
                return response;
        }

        private void validateConversationContext(Long productId, Long orderId) {
                if (productId != null && orderId != null) {
                        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品和订单上下文不能同时指定");
                }
        }

        private LocalDateTime resolveDeletionTimestamp(Map<ConversationContext, LocalDateTime> hiddenMap,
                                                       ConversationContext context) {
                LocalDateTime deletedAt = hiddenMap.get(context);
                if (deletedAt != null) {
                        return deletedAt;
                }
                return hiddenMap.get(context.withoutRelation());
        }

        private LocalDateTime resolveConversationDeletion(User owner,
                                                          User partner,
                                                          Long productId,
                                                          Long orderId) {
                return findSetting(owner.getId(), partner.getId(), productId, orderId)
                        .map(ConversationSetting::getDeletedAt)
                        .orElseGet(() -> conversationSettingRepository.findByOwnerIdAndPartnerId(owner.getId(), partner.getId())
                                .map(ConversationSetting::getDeletedAt)
                                .orElse(null));
        }

        private ConversationSetting getOrCreateSetting(User owner,
                                                       User partner,
                                                       Long productId,
                                                       Long orderId) {
                return findSetting(owner.getId(), partner.getId(), productId, orderId)
                        .orElseGet(() -> {
                                ConversationSetting setting = new ConversationSetting();
                                setting.setOwner(owner);
                                setting.setPartner(partner);
                                if (productId != null) {
                                        setting.setRelatedType(RelatedType.PRODUCT);
                                        setting.setRelatedId(productId);
                                } else if (orderId != null) {
                                        setting.setRelatedType(RelatedType.ORDER);
                                        setting.setRelatedId(orderId);
                                }
                                return setting;
                        });
        }

        private Optional<ConversationSetting> findSetting(Long ownerId,
                                                          Long partnerId,
                                                          Long productId,
                                                          Long orderId) {
                if (productId != null) {
                        return conversationSettingRepository.findByOwnerIdAndPartnerIdAndRelatedTypeAndRelatedId(
                                ownerId, partnerId, RelatedType.PRODUCT, productId);
                }
                if (orderId != null) {
                        return conversationSettingRepository.findByOwnerIdAndPartnerIdAndRelatedTypeAndRelatedId(
                                ownerId, partnerId, RelatedType.ORDER, orderId);
                }
                return conversationSettingRepository.findByOwnerIdAndPartnerId(ownerId, partnerId);
        }

        private Specification<Message> buildUnreadConversationSpec(Long receiverId,
                                                                    Long senderId,
                                                                    Long productId,
                                                                    Long orderId) {
                Specification<Message> spec = Specification.where((root, query, cb) -> cb.and(
                        cb.equal(root.get("receiver").get("id"), receiverId),
                        cb.equal(root.get("sender").get("id"), senderId),
                        cb.isFalse(root.get("read"))
                ));
                return applyRelationFilter(spec, productId, orderId);
        }

        private Specification<Message> applyRelationFilter(Specification<Message> spec,
                                                           Long productId,
                                                           Long orderId) {
                if (productId != null) {
                        return spec.and((root, query, cb) -> cb.and(
                                cb.equal(root.get("relatedType"), RelatedType.PRODUCT),
                                cb.equal(root.get("relatedId"), productId)
                        ));
                }
                if (orderId != null) {
                        return spec.and((root, query, cb) -> cb.and(
                                cb.equal(root.get("relatedType"), RelatedType.ORDER),
                                cb.equal(root.get("relatedId"), orderId)
                        ));
                }
                return spec;
        }

        private ConversationContext toContextKey(ConversationSetting setting) {
                return new ConversationContext(setting.getPartner().getId(), setting.getRelatedType(), setting.getRelatedId());
        }

        private static final class ConversationAccumulator {
                private final User partner;
                private Message lastMessage;
                private long unreadCount;

                private ConversationAccumulator(User partner) {
                        this.partner = partner;
                }

                public User getPartner() {
                        return partner;
                }

                public Message getLastMessage() {
                        return lastMessage;
                }

                public void setLastMessage(Message lastMessage) {
                        this.lastMessage = lastMessage;
                }

                public long getUnreadCount() {
                        return unreadCount;
                }

                public void incrementUnread() {
                        this.unreadCount++;
                }
        }

        private static final class ConversationContext {
                private final Long partnerId;
                private final RelatedType relatedType;
                private final Long relatedId;

                private ConversationContext(Long partnerId, RelatedType relatedType, Long relatedId) {
                        this.partnerId = partnerId;
                        this.relatedType = relatedType;
                        this.relatedId = relatedId;
                }

                static ConversationContext fromMessage(Long currentUserId, Message message) {
                        Long partnerId = message.getSender().getId().equals(currentUserId)
                                ? message.getReceiver().getId()
                                : message.getSender().getId();
                        return new ConversationContext(partnerId, message.getRelatedType(), message.getRelatedId());
                }

                ConversationContext withoutRelation() {
                        return new ConversationContext(partnerId, null, null);
                }

                RelatedType getRelatedType() {
                        return relatedType;
                }

                Long getRelatedId() {
                        return relatedId;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) {
                                return true;
                        }
                        if (o == null || getClass() != o.getClass()) {
                                return false;
                        }
                        ConversationContext that = (ConversationContext) o;
                        if (!partnerId.equals(that.partnerId)) {
                                return false;
                        }
                        if (relatedType != that.relatedType) {
                                return false;
                        }
                        return relatedId != null ? relatedId.equals(that.relatedId) : that.relatedId == null;
                }

                @Override
                public int hashCode() {
                        int result = partnerId.hashCode();
                        result = 31 * result + (relatedType != null ? relatedType.hashCode() : 0);
                        result = 31 * result + (relatedId != null ? relatedId.hashCode() : 0);
                        return result;
                }
        }
}
