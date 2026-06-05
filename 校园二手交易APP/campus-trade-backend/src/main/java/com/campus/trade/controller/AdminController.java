package com.campus.trade.controller;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.admin.AdminLoginRequest;
import com.campus.trade.dto.admin.AdminLoginResponse;
import com.campus.trade.dto.admin.AdminRegisterRequest;
import com.campus.trade.dto.admin.BatchUserFinalizeDeletionRequest;
import com.campus.trade.dto.admin.SystemNotificationRequest;
import com.campus.trade.dto.admin.UpdateUserStatusRequest;
import com.campus.trade.dto.message.MessageReportResponse;
import com.campus.trade.dto.message.ResolveReportRequest;
import com.campus.trade.dto.product.BatchProductReviewRequest;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.product.ProductReviewRequest;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.ReportStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.service.AdminService;
import com.campus.trade.service.MessageService;
import com.campus.trade.service.NotificationService;
import com.campus.trade.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "管理员接口", description = "管理员相关功能接口")
public class AdminController {

    private final AdminService adminService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;

    public AdminController(AdminService adminService,
                           MessageService messageService,
                           NotificationService notificationService,
                           IdempotencyService idempotencyService) {
        this.adminService = adminService;
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员用户登录系统")
    @OperationLog(title = "管理员登录", action = "ADMIN_LOGIN", type = OperationType.OTHER)
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminService.login(request));
    }

    @PreAuthorize("hasAuthority('ADMIN_ADMIN_MANAGEMENT')")
    @PostMapping("/register")
    @Operation(summary = "创建管理员", description = "超级管理员创建新的管理员账户")
    public ApiResponse<Void> register(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                    @Valid @RequestBody AdminRegisterRequest request) {
        idempotencyService.execute(
                idempotencyKey,
                "admin",
                "ADMIN_REGISTER",
                request,
                () -> {
                    adminService.register(request);
                    return null;
                },
                Void.class);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAuthority('ADMIN_PRODUCT_REVIEW')")
    @GetMapping("/products/pending")
    @Operation(summary = "待审核商品列表", description = "获取待审核的商品列表")
    public ApiResponse<PaginatedResponse<ProductResponse>> pendingProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listPendingProducts(page, size));
    }

    @PreAuthorize("hasAuthority('ADMIN_PRODUCT_REVIEW')")
    @PatchMapping("/products/{id}/review")
    @Operation(summary = "商品审核", description = "审核商品，决定是否通过")
    @OperationLog(title = "商品审核", action = "REVIEW_PRODUCT", type = OperationType.REVIEW,
            resourceId = "#{#id}")
    public ApiResponse<ProductResponse> reviewProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Valid @RequestBody ProductReviewRequest request) {
        return ApiResponse.success(adminService.reviewProduct(id, request));
    }

    @PreAuthorize("hasAuthority('ADMIN_PRODUCT_REVIEW')")
    @PostMapping("/products/review/batch")
    @Operation(summary = "批量审核商品", description = "批量审核商品，决定是否通过")
    @OperationLog(title = "批量审核商品", action = "REVIEW_PRODUCT_BATCH", type = OperationType.REVIEW)
    public ApiResponse<BatchOperationResult> batchReviewProducts(@Valid @RequestBody BatchProductReviewRequest request) {
        return ApiResponse.success(adminService.batchReviewProducts(request));
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGEMENT')")
    @GetMapping("/users")
    @Operation(summary = "用户列表", description = "获取用户列表，支持按状态和角色筛选")
    public ApiResponse<PaginatedResponse<UserSummary>> users(
            @Parameter(description = "用户状态") @RequestParam(required = false) AccountStatus status,
            @Parameter(description = "用户角色") @RequestParam(required = false) UserRole role,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listUsers(status, role, page, size));
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGEMENT')")
    @PatchMapping("/users/{id}/status")
    @Operation(summary = "更新用户状态", description = "更新用户账户状态（如封禁、解封等）")
        @OperationLog(title = "用户管理", action = "UPDATE_USER_STATUS", type = OperationType.UPDATE,
            resourceId = "#{#id}")
    public ApiResponse<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminService.updateUserStatus(id, request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGEMENT')")
    @PostMapping("/users/{id}/delete/finalize")
    @Operation(summary = "立即完成账号注销", description = "管理员手动执行用户账号注销，绕过倒计时")
    @OperationLog(title = "账号注销", action = "FINALIZE_DELETION", type = OperationType.DELETE,
            resourceId = "#{#id}")
    public ApiResponse<Void> finalizeUserDeletion(@Parameter(description = "用户ID") @PathVariable Long id) {
        adminService.finalizeUserDeletion(id);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGEMENT')")
    @PostMapping("/users/delete/finalize/batch")
    @Operation(summary = "批量完成账号注销", description = "管理员批量执行用户账号注销，绕过倒计时")
    @OperationLog(title = "批量账号注销", action = "FINALIZE_DELETION_BATCH", type = OperationType.DELETE)
    public ApiResponse<BatchOperationResult> batchFinalizeUserDeletion(
            @Valid @RequestBody BatchUserFinalizeDeletionRequest request) {
        return ApiResponse.success(adminService.batchFinalizeUserDeletion(request));
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGEMENT')")
    @PostMapping("/users/{id}/delete/cancel")
    @Operation(summary = "取消账号注销申请", description = "管理员撤销用户的注销流程，恢复正常状态")
    @OperationLog(title = "账号注销", action = "CANCEL_DELETION", type = OperationType.UPDATE,
            resourceId = "#{#id}")
    public ApiResponse<Void> cancelUserDeletion(@Parameter(description = "用户ID") @PathVariable Long id) {
        adminService.cancelUserDeletion(id);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAuthority('ADMIN_MESSAGE_REVIEW')")
    @GetMapping("/messages/reports")
    @Operation(summary = "消息举报列表", description = "获取用户举报的消息列表")
    public ApiResponse<PaginatedResponse<MessageReportResponse>> reports(
            @Parameter(description = "举报状态") @RequestParam(required = false) ReportStatus status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(messageService.listReports(status, page, size));
    }

    @PreAuthorize("hasAuthority('ADMIN_MESSAGE_REVIEW')")
    @PatchMapping("/messages/reports/{id}")
    @Operation(summary = "处理举报", description = "处理用户举报的消息")
    @OperationLog(title = "举报处理", action = "RESOLVE_REPORT", type = OperationType.REVIEW,
            resourceId = "#{#id}")
    public ApiResponse<Void> resolveReport(
            @Parameter(description = "举报ID") @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request) {
        messageService.resolveReport(id, request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAuthority('ADMIN_NOTIFICATION_MANAGEMENT')")
    @PostMapping("/notifications")
    @Operation(summary = "创建系统通知", description = "创建并向用户发送系统通知")
    @OperationLog(title = "系统通知", action = "CREATE_NOTIFICATION", type = OperationType.NOTIFY)
    public ApiResponse<Void> createNotification(@Valid @RequestBody SystemNotificationRequest request) {
        notificationService.createNotification(request);
        return ApiResponse.success();
    }
}
