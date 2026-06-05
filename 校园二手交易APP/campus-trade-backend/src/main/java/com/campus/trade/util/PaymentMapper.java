package com.campus.trade.util;

import com.campus.trade.dto.payment.PaymentRecordResponse;
import com.campus.trade.dto.payment.PaymentStatusEventResponse;
import com.campus.trade.model.entity.Payment;
import com.campus.trade.model.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    public static PaymentRecordResponse toRecord(Payment payment, ObjectMapper objectMapper) {
        PaymentRecordResponse response = new PaymentRecordResponse();
        response.setPaymentId(payment.getId());
        response.setReferenceNo(payment.getReferenceNo());
        response.setStatus(payment.getStatus());
        response.setMethod(payment.getMethod());
        response.setChannel(payment.getChannel());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setExpiresAt(payment.getExpiresAt());
        response.setStatusTimeline(buildTimeline(payment));
        response.setRequestPayload(parsePayload(objectMapper, payment.getRequestPayload()));
        response.setResponsePayload(parsePayload(objectMapper, payment.getResponsePayload()));
        response.setWebhookStatus(payment.getWebhookStatus());
        response.setCallbackAt(payment.getCallbackAt());
        response.setCreateTime(payment.getCreateTime());
        response.setUpdateTime(payment.getUpdateTime());
        return response;
    }

    private static List<PaymentStatusEventResponse> buildTimeline(Payment payment) {
        List<PaymentStatusEventResponse> timeline = new ArrayList<>();
        LocalDateTime created = payment.getCreateTime();
        if (created != null) {
            timeline.add(new PaymentStatusEventResponse(PaymentStatus.PENDING, created));
        }
        if (payment.getStatus() != null && payment.getStatus() != PaymentStatus.PENDING) {
            LocalDateTime statusTime = payment.getCallbackAt() != null ? payment.getCallbackAt() : payment.getUpdateTime();
            if (statusTime != null) {
                timeline.add(new PaymentStatusEventResponse(payment.getStatus(), statusTime));
            }
        }
        return timeline;
    }

    private static Map<String, Object> parsePayload(ObjectMapper objectMapper, String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析支付 JSON 失败", e);
        }
    }
}
