package com.campus.trade.service;

import com.campus.trade.dto.payment.PaymentIntentRequest;
import com.campus.trade.dto.payment.PaymentIntentResponse;
import com.campus.trade.dto.payment.PaymentRecordResponse;
import com.campus.trade.dto.payment.PaymentSimulationRequest;
import com.campus.trade.dto.payment.PaymentSimulationResult;
import com.campus.trade.dto.payment.PaymentStatusResponse;
import com.campus.trade.dto.payment.PaymentWebhookRequest;
import com.campus.trade.dto.payment.PaymentWebhookStatus;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Payment;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.PaymentRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.PaymentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

    private static final long DEFAULT_EXPIRE_MINUTES = 15L;
    private static final Set<String> SUPPORTED_METHODS = Set.of("ALIPAY", "WECHAT", "CASH");
    private static final Set<String> SUPPORTED_CHANNELS = Set.of("MOCK_QR", "MOCK_TRANSFER", "CASH");
    private static final Set<PaymentStatus> ACTIVE_STATUSES = Set.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING);
    private static final String MOCK_WEBHOOK_SECRET = "mock-payment-secret";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final boolean alwaysSuccess;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          ObjectMapper objectMapper,
                          EmailNotificationService emailNotificationService,
                          NotificationService notificationService,
                          @Value("${payment.mock.always-success:false}") boolean alwaysSuccess) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.emailNotificationService = emailNotificationService;
        this.notificationService = notificationService;
        this.alwaysSuccess = alwaysSuccess;
    }

    @Transactional
    public PaymentIntentResponse createIntent(String username, Long orderId, PaymentIntentRequest request) {
        User buyer = loadUser(username);
        Order order = getOrder(orderId);
        validateOrderOwnership(order, buyer);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不可支付");
        }
        String method = normalizeMethod(request.getMethod());
        String channel = normalizeChannel(request.getChannel(), method);
        cancelActiveIntents(order.getId());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setBuyer(buyer);
        payment.setAmount(order.getPrice());
        payment.setCurrency("CNY");
        payment.setMethod(method);
        payment.setChannel(channel);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setReferenceNo(generateReferenceNo());
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES));

        Map<String, Object> payload = buildPayload(payment);
        payment.setRequestPayload(writePayload(payload));
        paymentRepository.save(payment);

        if (alwaysSuccess && !"CASH".equalsIgnoreCase(channel)) {
            markPaymentSucceeded(payment, "MOCK_AUTO", Collections.emptyMap());
        }

        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(method);
        order.setPaymentReference(payment.getReferenceNo());
        order.setPaymentExpireTime(payment.getExpiresAt());
        order.setPaymentMetadata(payment.getRequestPayload());

        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setPaymentId(payment.getId());
        response.setOrderId(order.getId());
        response.setReferenceNo(payment.getReferenceNo());
        response.setStatus(payment.getStatus());
        response.setMethod(method);
        response.setChannel(channel);
        response.setAmount(order.getPrice());
        response.setCurrency(payment.getCurrency());
        response.setExpiresAt(payment.getExpiresAt());
        response.setQrCodeUrl((String) payload.get("qrCodeUrl"));
        response.setPayload(payload);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String username, Long orderId, boolean includeHistory) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        boolean isBuyer = order.getBuyer().getUsername().equals(username);
        boolean isSeller = order.getSeller().getUsername().equals(username);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权查看支付信息");
        }
        return buildStatusResponse(order, includeHistory);
    }

    @Transactional
    public void cancelPayment(String username, Long paymentId) {
        User buyer = loadUser(username);
        Payment payment = paymentRepository.findByIdAndBuyerId(paymentId, buyer.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!ACTIVE_STATUSES.contains(payment.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "支付状态不可取消");
        }
        markPaymentFailed(payment, PaymentStatus.CANCELLED, "USER", "USER_CANCELLED", Map.of());
    }

    @Transactional
    public PaymentStatusResponse simulatePayment(String username, Long paymentId, PaymentSimulationRequest request) {
        User buyer = loadUser(username);
        Payment payment = paymentRepository.findByIdAndBuyerId(paymentId, buyer.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (isTerminal(payment.getStatus())) {
            return buildStatusResponse(payment.getOrder(), true);
        }
        switch (request.getResult()) {
            case SUCCESS -> markPaymentSucceeded(payment, "SIMULATION", Map.of());
            case FAILURE -> markPaymentFailed(payment, PaymentStatus.FAILED, "SIMULATION", "SIMULATION_FAILURE", Map.of());
            case EXPIRED -> markPaymentFailed(payment, PaymentStatus.EXPIRED, "SIMULATION", "SIMULATION_EXPIRED", Map.of());
            default -> throw new BusinessException(ErrorCode.BUSINESS_ERROR, "模拟结果无效");
        }
        return buildStatusResponse(payment.getOrder(), true);
    }

    @Transactional
    public void handleMockWebhook(PaymentWebhookRequest request) {
        validateSignature(request);
        Payment payment = paymentRepository.findByReferenceNo(request.getReferenceNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (isTerminal(payment.getStatus())) {
            return;
        }
        payment.setWebhookStatus(request.getStatus().name());
        Map<String, Object> payload = request.getPayload();
        if (request.getStatus() == PaymentWebhookStatus.SUCCESS) {
            markPaymentSucceeded(payment, "WEBHOOK", payload);
        } else if (request.getStatus() == PaymentWebhookStatus.EXPIRED) {
            markPaymentFailed(payment, PaymentStatus.EXPIRED, "WEBHOOK", "WEBHOOK_EXPIRED", payload);
        } else {
            markPaymentFailed(payment, PaymentStatus.FAILED, "WEBHOOK", "WEBHOOK_FAILURE", payload);
        }
    }

    private void cancelActiveIntents(Long orderId) {
        List<Payment> activePayments = paymentRepository.findByOrderIdAndStatusIn(orderId, ACTIVE_STATUSES);
        for (Payment payment : activePayments) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setResponsePayload(writePayload(Map.of("status", "CANCELLED", "reason", "SUPERSEDED", "source", "SYSTEM")));
            Order order = payment.getOrder();
            if (order != null && payment.getReferenceNo().equals(order.getPaymentReference())) {
                order.setPaymentStatus(PaymentStatus.CANCELLED);
                resetOrderPaymentSnapshot(order);
                order.setPaymentMetadata(payment.getResponsePayload());
            }
        }
    }

    private void markPaymentSucceeded(Payment payment, String source, Map<String, Object> extraPayload) {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setResponsePayload(writePayload(buildResponsePayload("SUCCESS", source, null, extraPayload)));
        payment.setCallbackAt(LocalDateTime.now());
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order.setPaymentReference(payment.getReferenceNo());
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod(payment.getMethod());
        order.setPaymentExpireTime(null);
        order.setPaymentMetadata(payment.getResponsePayload());
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING_SHIPMENT);
        }
        // 更新商品状态为已售出
        Product product = order.getProduct();
        if (product != null) {
            product.setStatus(ProductStatus.SOLD);
        }
        emailNotificationService.notifyPaymentSucceeded(order);
        notifyPaymentSucceeded(order);
    }

    private void markPaymentFailed(Payment payment,
                                   PaymentStatus failureStatus,
                                   String source,
                                   String reason,
                                   Map<String, Object> extraPayload) {
        payment.setStatus(failureStatus);
        payment.setResponsePayload(writePayload(buildResponsePayload(failureStatus.name(), source, reason, extraPayload)));
        payment.setCallbackAt(LocalDateTime.now());
        Order order = payment.getOrder();
        if (order.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
            order.setPaymentStatus(failureStatus);
            resetOrderPaymentSnapshot(order);
            order.setPaymentMetadata(payment.getResponsePayload());
        }
        notifyPaymentFailed(order, failureStatus);
    }

    private Map<String, Object> buildResponsePayload(String status,
                                                     String source,
                                                     String reason,
                                                     Map<String, Object> extraPayload) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        if (StringUtils.hasText(source)) {
            payload.put("source", source);
        }
        if (StringUtils.hasText(reason)) {
            payload.put("reason", reason);
        }
        if (extraPayload != null && !extraPayload.isEmpty()) {
            payload.put("payload", extraPayload);
        }
        return payload;
    }

    private void resetOrderPaymentSnapshot(Order order) {
        order.setPaymentReference(null);
        order.setPaymentExpireTime(null);
        order.setPaymentTime(null);
    }

    private PaymentStatusResponse buildStatusResponse(Order order, boolean includeHistory) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setOrderId(order.getId());
        response.setOrderStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentReference(order.getPaymentReference());
        response.setPaymentTime(order.getPaymentTime());
        response.setPaymentExpireTime(order.getPaymentExpireTime());
        response.setPaymentMetadata(readPayload(order.getPaymentMetadata()));
        response.setRefundStatus(order.getRefundStatus() == null ? RefundStatus.NONE : order.getRefundStatus());
        response.setRefundTime(order.getRefundTime());

        List<Payment> records = includeHistory
                ? paymentRepository.findByOrderIdOrderByCreateTimeDesc(order.getId())
                : paymentRepository.findTopByOrderIdOrderByCreateTimeDesc(order.getId())
                .map(List::of)
                .orElseGet(Collections::emptyList);

        response.setLatestIntent(records.isEmpty() ? null : toIntentResponse(records.get(0)));
        List<PaymentRecordResponse> history = records.stream()
                .map(payment -> PaymentMapper.toRecord(payment, objectMapper))
                .toList();
        response.setHistory(history);
        return response;
    }

    private Map<String, Object> buildPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", payment.getChannel());
        payload.put("referenceNo", payment.getReferenceNo());
        if (!"CASH".equalsIgnoreCase(payment.getMethod())) {
            payload.put("qrCodeUrl", "https://mock.campus-trade/pay/" + payment.getReferenceNo());
        }
        return payload;
    }

    private String writePayload(Map<String, Object> payload) {
        String json = serializePayload(payload);
        return StringUtils.hasText(json) ? json : null;
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化支付记录失败", e);
        }
    }

    private Map<String, Object> readPayload(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析支付 JSON 失败", e);
        }
    }

    private PaymentIntentResponse toIntentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setPaymentId(payment.getId());
        response.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
        response.setReferenceNo(payment.getReferenceNo());
        response.setStatus(payment.getStatus());
        response.setMethod(payment.getMethod());
        response.setChannel(payment.getChannel());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setExpiresAt(payment.getExpiresAt());
        Map<String, Object> payload = readPayload(payment.getRequestPayload());
        response.setPayload(payload);
        if (payload != null) {
            Object qr = payload.get("qrCodeUrl");
            if (qr instanceof String qrUrl) {
                response.setQrCodeUrl(qrUrl);
            }
        }
        return response;
    }

    private void validateSignature(PaymentWebhookRequest request) {
        if (!StringUtils.hasText(request.getSignature())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "缺少签名");
        }
        String payloadJson = serializePayload(request.getPayload());
        String canonical = request.getReferenceNo() + "|" + request.getStatus().name() + "|" + payloadJson + "|" + MOCK_WEBHOOK_SECRET;
        String expected = sha256Hex(canonical);
        if (!expected.equalsIgnoreCase(request.getSignature())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "签名无效");
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte aByte : bytes) {
                builder.append(String.format("%02x", aByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("缺少 SHA-256 算法", e);
        }
    }

    private String normalizeMethod(String method) {
        if (!StringUtils.hasText(method)) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "支付方式必填");
        }
        String normalized = method.trim().toUpperCase();
        if (!SUPPORTED_METHODS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "暂不支持该支付方式");
        }
        return normalized;
    }

    private String normalizeChannel(String channel, String method) {
        if ("CASH".equalsIgnoreCase(method)) {
            return "CASH";
        }
        if (!StringUtils.hasText(channel)) {
            return "MOCK_QR";
        }
        String normalized = channel.trim().toUpperCase();
        if (!SUPPORTED_CHANNELS.contains(normalized)) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "渠道无效");
        }
        return normalized;
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.REFUNDED;
    }

    private String generateReferenceNo() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateOrderOwnership(Order order, User buyer) {
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权操作该订单");
        }
    }

    private void notifyPaymentSucceeded(Order order) {
        notificationService.notifyUser(order.getBuyer().getId(), "支付成功",
                String.format("订单 %s 已支付成功，卖家将尽快发货。", order.getOrderNo()));
        notificationService.notifyUser(order.getSeller().getId(), "买家已完成支付",
                String.format("买家 %s 已支付订单 %s，请尽快安排交付。",
                        order.getBuyer().getUsername(), order.getOrderNo()));
    }

    private void notifyPaymentFailed(Order order, PaymentStatus status) {
        if (order == null || status == null) {
            return;
        }
        notificationService.notifyUser(order.getBuyer().getId(), "支付未完成",
                String.format("订单 %s 支付状态更新为 %s，请重新尝试或联系卖家。",
                        order.getOrderNo(), status.name()));
    }

    }
