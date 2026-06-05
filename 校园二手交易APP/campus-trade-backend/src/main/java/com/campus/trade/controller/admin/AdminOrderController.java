package com.campus.trade.controller.admin;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.common.BatchOperationResult;
import com.campus.trade.dto.order.BatchUpdateOrderStatusRequest;
import com.campus.trade.dto.order.OrderResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "管理员订单列表", description = "管理员查看订单列表（支持按状态筛选）")
    public ApiResponse<PaginatedResponse<OrderResponse>> listOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.adminListOrders(status, page, size));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "管理员查看订单详情", description = "管理员查看指定订单的详细信息")
    public ApiResponse<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderDetailForAdmin(orderId));
    }

    @PostMapping("/batch/status")
    @Operation(summary = "批量更新订单状态", description = "管理员批量处理订单：支持各种订单状态")
    @OperationLog(title = "订单管理", action = "ORDER_STATUS_BATCH", type = OperationType.UPDATE)
    public ApiResponse<BatchOperationResult> batchUpdateStatus(@Valid @RequestBody BatchUpdateOrderStatusRequest request) {
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无效的订单状态");
        }

        return ApiResponse.success(orderService.adminBatchUpdateStatus(request.getIds(), status));
    }

    @GetMapping("/export")
    @Operation(summary = "导出订单数据", description = "导出指定条件的订单数据")
    public ApiResponse<Void> exportOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endDate) {
        // 订单导出功能开发中，返回成功状态
        return ApiResponse.success();
    }
}
