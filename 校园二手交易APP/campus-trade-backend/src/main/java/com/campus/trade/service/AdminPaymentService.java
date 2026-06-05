package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.payment.PaymentRecordResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.Payment;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.RefundStatus;
import com.campus.trade.repository.PaymentRepository;
import com.campus.trade.util.PaymentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public AdminPaymentService(PaymentRepository paymentRepository, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PaymentRecordResponse> listPayments(String orderNo,
                                                                 String buyerPhone,
                                                                 PaymentStatus status,
                                                                 LocalDateTime dateFrom,
                                                                 LocalDateTime dateTo,
                                                                 int page,
                                                                 int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<Payment> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(orderNo)) {
                predicates.add(cb.equal(root.join("order").get("orderNo"), orderNo));
            }
            if (StringUtils.hasText(buyerPhone)) {
                predicates.add(cb.equal(root.join("buyer").get("phone"), buyerPhone));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), dateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Payment> pageResult = paymentRepository.findAll(specification, pageable);
        List<PaymentRecordResponse> items = pageResult.getContent().stream()
                .map(payment -> PaymentMapper.toRecord(payment, objectMapper))
                .toList();
        return PaginatedResponse.of(items, safePage, safeSize, pageResult.getTotalElements());
    }

    @Transactional
    public void triggerRefund(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "仅已成功的支付可退款");
        }
        Order order = payment.getOrder();
        String resolvedReason = StringUtils.hasText(reason) ? reason : "管理员触发退款";
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment.setResponsePayload(writePayload(Map.of("status", "REFUND_PENDING", "source", "ADMIN", "reason", resolvedReason)));
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setResponsePayload(writePayload(Map.of("status", "REFUNDED", "source", "ADMIN", "reason", resolvedReason)));
        payment.setCallbackAt(LocalDateTime.now());

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundStatus(RefundStatus.REFUNDED);
        order.setRefundTime(LocalDateTime.now());
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentReference(null);
        order.setPaymentExpireTime(null);
        order.setPaymentTime(null);
        order.setPaymentMetadata(payment.getResponsePayload());
        if (order.getProduct() != null) {
            order.getProduct().setStatus(ProductStatus.ON_SALE);
        }
    }

    @Transactional
    public void forceClose(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "已完成的支付不可关闭");
        }
        String resolvedReason = StringUtils.hasText(reason) ? reason : "管理员关闭支付";
        payment.setStatus(PaymentStatus.FAILED);
        payment.setResponsePayload(writePayload(Map.of("status", "FAILED", "source", "ADMIN", "reason", resolvedReason)));
        payment.setCallbackAt(LocalDateTime.now());

        Order order = payment.getOrder();
        if (order.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentReference(null);
            order.setPaymentExpireTime(null);
            order.setPaymentTime(null);
            order.setPaymentMetadata(payment.getResponsePayload());
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化支付回执失败", e);
        }
    }
}
