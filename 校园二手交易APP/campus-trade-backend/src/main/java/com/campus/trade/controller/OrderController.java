package com.campus.trade.controller;

import java.util.List;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.order.BatchOrderCreateResponse;
import com.campus.trade.dto.order.CartCheckoutRequest;
import com.campus.trade.dto.order.CreateOrderRequest;
import com.campus.trade.dto.order.OrderActionRequest;
import com.campus.trade.dto.order.OrderResponse;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.IdempotencyService;
import com.campus.trade.service.OrderService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/orders")
@Tag(name = "订单接口", description = "订单相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "创建新的订单")
    @Timed(value = "api.orders.create", histogram = true)
    public ApiResponse<OrderResponse> create(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                             @Valid @RequestBody CreateOrderRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        OrderResponse response = idempotencyService.execute(
                idempotencyKey,
            username,
                "ORDER_CREATE",
                request,
            () -> orderService.createOrder(username, request),
                OrderResponse.class);
        return ApiResponse.success(response);
    }

    @PostMapping("/cart/checkout")
    @Operation(summary = "购物车批量结算", description = "从购物车中选择多个商品批量下单")
    public ApiResponse<BatchOrderCreateResponse> checkoutFromCart(@Valid @RequestBody CartCheckoutRequest request) {
        return ApiResponse.success(orderService.checkoutFromCart(SecurityUtils.getCurrentUsername(), request));
    }

    @GetMapping("/me")
    @Operation(summary = "我的订单", description = "获取当前用户购买的订单列表")
    @Timed(value = "api.orders.mine", histogram = true)
    public ApiResponse<List<OrderResponse>> myOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) OrderStatus status) {
        // 设置page=1, size=100，获取所有订单
        return ApiResponse.success(orderService.listOrders(SecurityUtils.getCurrentUsername(), false, status, 1, 100).getItems());
    }

    @GetMapping("/sold")
    @Operation(summary = "我售出的订单", description = "获取当前用户售出的订单列表")
    @Timed(value = "api.orders.sold", histogram = true)
    public ApiResponse<List<OrderResponse>> soldOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) OrderStatus status) {
        // 设置page=1, size=100，获取所有订单
        return ApiResponse.success(orderService.listOrders(SecurityUtils.getCurrentUsername(), true, status, 1, 100).getItems());
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "获取指定订单的详细信息")
    @Timed(value = "api.orders.detail", histogram = true)
    public ApiResponse<OrderResponse> detail(@Parameter(description = "订单ID") @PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderDetail(SecurityUtils.getCurrentUsername(), id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "卖家确认订单", description = "卖家确认并开始处理订单，状态变为待发货")
    @Timed(value = "api.orders.confirm", histogram = true)
    public ApiResponse<Void> confirm(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.confirmOrder(SecurityUtils.getCurrentUsername(), id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝订单", description = "卖家拒绝订单")
    @Timed(value = "api.orders.reject", histogram = true)
    public ApiResponse<Void> reject(@Parameter(description = "订单ID") @PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        orderService.rejectOrder(SecurityUtils.getCurrentUsername(), id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "买家取消订单", description = "买家在未发货前取消订单")
    @Timed(value = "api.orders.cancel", histogram = true)
    public ApiResponse<Void> cancel(@Parameter(description = "订单ID") @PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        orderService.cancelOrder(SecurityUtils.getCurrentUsername(), id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "买家完成订单", description = "买家确认收货并评价订单")
    @Timed(value = "api.orders.complete", histogram = true)
    public ApiResponse<Void> complete(@Parameter(description = "订单ID") @PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        orderService.completeOrder(SecurityUtils.getCurrentUsername(), id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/seller-review")
    @Operation(summary = "卖家评价买家", description = "卖家在订单完成后对买家进行评价")
    @Timed(value = "api.orders.sellerReview", histogram = true)
    public ApiResponse<Void> sellerReview(@Parameter(description = "订单ID") @PathVariable Long id, @Valid @RequestBody OrderActionRequest request) {
        orderService.sellerReviewBuyer(SecurityUtils.getCurrentUsername(), id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单", description = "删除指定的订单")
    @Timed(value = "api.orders.delete", histogram = true)
    public ApiResponse<Void> delete(@Parameter(description = "订单ID") @PathVariable Long id) {
        orderService.deleteOrder(SecurityUtils.getCurrentUsername(), id);
        return ApiResponse.success();
    }
}
