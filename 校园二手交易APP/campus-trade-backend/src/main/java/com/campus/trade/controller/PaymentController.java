package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.payment.PaymentIntentRequest;
import com.campus.trade.dto.payment.PaymentIntentResponse;
import com.campus.trade.dto.payment.PaymentSimulationRequest;
import com.campus.trade.dto.payment.PaymentStatusResponse;
import com.campus.trade.dto.payment.PaymentWebhookRequest;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.IdempotencyService;
import com.campus.trade.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "支付接口", description = "支付意图、模拟与回调接口")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/orders/{orderId}/intent")
    @Operation(summary = "创建支付意图")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<PaymentIntentResponse> createIntent(@PathVariable Long orderId,
                                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                           @Valid @RequestBody PaymentIntentRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        PaymentIntentResponse response = idempotencyService.execute(
                idempotencyKey,
                username,
                "PAYMENT_INTENT_CREATE",
                request,
                () -> paymentService.createIntent(username, orderId, request),
                PaymentIntentResponse.class);
        return ApiResponse.success(response);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "查询订单支付状态")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<PaymentStatusResponse> getStatus(@PathVariable Long orderId,
                                                        @RequestParam(defaultValue = "false") boolean includeHistory) {
        return ApiResponse.success(paymentService.getPaymentStatus(SecurityUtils.getCurrentUsername(), orderId, includeHistory));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "取消支付意图")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ResponseEntity<Void> cancel(@PathVariable Long paymentId) {
        paymentService.cancelPayment(SecurityUtils.getCurrentUsername(), paymentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{paymentId}/simulate")
    @Operation(summary = "模拟支付结果（测试）")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<PaymentStatusResponse> simulate(@PathVariable Long paymentId,
                                                       @Valid @RequestBody PaymentSimulationRequest request) {
        return ApiResponse.success(paymentService.simulatePayment(SecurityUtils.getCurrentUsername(), paymentId, request));
    }

    @PostMapping("/webhook/mock")
    @Operation(summary = "模拟支付回调")
    @PreAuthorize("permitAll()")
    public ApiResponse<Void> mockWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        paymentService.handleMockWebhook(request);
        return ApiResponse.success();
    }
}
