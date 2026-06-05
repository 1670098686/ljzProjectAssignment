package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.message.ConversationRequest;
import com.campus.trade.dto.message.ConversationSummaryResponse;
import com.campus.trade.dto.message.MessageResponse;
import com.campus.trade.dto.message.ReadMessagesRequest;
import com.campus.trade.dto.message.ReportMessageRequest;
import com.campus.trade.dto.message.SendMessageRequest;
import com.campus.trade.dto.message.SystemNotificationResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.MessageService;
import com.campus.trade.service.NotificationService;
import com.campus.trade.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "消息接口", description = "消息相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class MessageController {

    private final MessageService messageService;
    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;

    public MessageController(MessageService messageService,
                             NotificationService notificationService,
                             IdempotencyService idempotencyService) {
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "发送消息", description = "发送消息给指定用户")
    public ApiResponse<MessageResponse> send(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                           @Valid @RequestBody SendMessageRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        MessageResponse response = idempotencyService.execute(
                idempotencyKey,
                username,
                "MESSAGE_SEND",
                request,
                () -> messageService.sendMessage(username, request),
                MessageResponse.class);
        return ApiResponse.success(response);
    }

    @PostMapping("/report")
    @Operation(summary = "举报消息", description = "举报不当消息")
    public ApiResponse<Void> report(@Valid @RequestBody ReportMessageRequest request) {
        messageService.reportMessage(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表", description = "获取用户的对话列表")
    public ApiResponse<PaginatedResponse<ConversationSummaryResponse>> conversations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(messageService.listConversations(SecurityUtils.getCurrentUsername(), page, size));
    }

    @GetMapping("/history")
    @Operation(summary = "获取聊天历史", description = "获取与指定用户的聊天历史记录")
    public ApiResponse<PaginatedResponse<MessageResponse>> history(
            @Parameter(description = "目标用户ID") @RequestParam Long toUserId,
            @Parameter(description = "商品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(messageService.conversationHistory(SecurityUtils.getCurrentUsername(), toUserId, productId, orderId, page, size));
    }

    @PostMapping("/read")
    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读")
    public ApiResponse<Void> markRead(@Valid @RequestBody ReadMessagesRequest request) {
        messageService.markMessagesRead(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @PostMapping("/conversations/read")
    @Operation(summary = "标记对话已读", description = "将与指定用户的整个对话标记为已读")
    public ApiResponse<Void> markConversationRead(@Valid @RequestBody ConversationRequest request) {
        messageService.markConversationRead(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/conversations")
    @Operation(summary = "删除对话", description = "删除与指定用户的对话记录")
    public ApiResponse<Void> deleteConversation(@Valid @RequestBody ConversationRequest request) {
        messageService.deleteConversation(SecurityUtils.getCurrentUsername(), request);
        return ApiResponse.success();
    }

    @GetMapping("/system")
    @Operation(summary = "获取系统通知", description = "获取发送给用户的系统通知")
    public ApiResponse<PaginatedResponse<SystemNotificationResponse>> systemNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationService.listNotifications(SecurityUtils.getCurrentUsername(), page, size));
    }

    @PostMapping("/system/{id}/read")
    @Operation(summary = "标记系统通知已读", description = "将指定系统通知标记为已读")
    public ApiResponse<Void> readSystemNotification(@Parameter(description = "通知ID") @PathVariable Long id) {
        notificationService.markRead(SecurityUtils.getCurrentUsername(), id);
        return ApiResponse.success();
    }
}
