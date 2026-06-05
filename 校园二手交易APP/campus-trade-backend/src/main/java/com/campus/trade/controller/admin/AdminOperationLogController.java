package com.campus.trade.controller.admin;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.admin.OperationLogResponse;
import com.campus.trade.model.enums.OperationResult;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.OperationLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@PreAuthorize(AccessExpressions.ADMIN)
@Tag(name = "管理员操作日志", description = "管理员操作日志查询")
public class AdminOperationLogController {

    private final OperationLogQueryService queryService;

    public AdminOperationLogController(OperationLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "操作日志列表", description = "分页查询管理员操作日志，支持按操作者/动作/类型/结果筛选")
    public ApiResponse<PaginatedResponse<OperationLogResponse>> list(
            @Parameter(description = "操作者（模糊匹配）") @RequestParam(required = false) String operator,
            @Parameter(description = "动作（模糊匹配）") @RequestParam(required = false) String action,
            @Parameter(description = "操作类型") @RequestParam(required = false) OperationType type,
            @Parameter(description = "执行结果") @RequestParam(required = false) OperationResult result,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(queryService.list(operator, action, type, result, page, size));
    }
}
