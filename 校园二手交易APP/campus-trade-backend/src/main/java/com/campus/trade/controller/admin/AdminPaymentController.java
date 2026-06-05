package com.campus.trade.controller.admin;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.payment.AdminPaymentActionRequest;
import com.campus.trade.dto.payment.PaymentRecordResponse;
import com.campus.trade.model.enums.PaymentStatus;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.service.AdminPaymentService;
import com.campus.trade.security.AccessExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize(AccessExpressions.ADMIN)
@Tag(name = "管理员-支付管理")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping
    @Operation(summary = "支付记录分页查询")
    public ApiResponse<PaginatedResponse<PaymentRecordResponse>> list(@RequestParam(required = false) String orderNo,
                                                                      @RequestParam(required = false) String buyerPhone,
                                                                      @RequestParam(required = false) PaymentStatus status,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
                                                                      @RequestParam(defaultValue = "1") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminPaymentService.listPayments(orderNo, buyerPhone, status, dateFrom, dateTo, page, size));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "管理员触发退款")
        @OperationLog(title = "支付管理", action = "REFUND_PAYMENT", type = OperationType.UPDATE,
            resourceId = "#{#paymentId}")
    public ApiResponse<Void> refund(@PathVariable Long paymentId,
                                    @RequestBody(required = false) AdminPaymentActionRequest request) {
        adminPaymentService.triggerRefund(paymentId, request != null ? request.getReason() : null);
        return ApiResponse.success();
    }

    @PostMapping("/{paymentId}/close")
    @Operation(summary = "管理员关闭异常支付")
        @OperationLog(title = "支付管理", action = "CLOSE_PAYMENT", type = OperationType.UPDATE,
            resourceId = "#{#paymentId}")
    public ApiResponse<Void> close(@PathVariable Long paymentId,
                                   @RequestBody(required = false) AdminPaymentActionRequest request) {
        adminPaymentService.forceClose(paymentId, request != null ? request.getReason() : null);
        return ApiResponse.success();
    }
}
