package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.BudgetDto;
import com.finance.dto.CreateBudgetRequest;
import com.finance.dto.UpdateBudgetRequest;
import com.finance.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "预算管理", description = "预算设置、查询和管理的接口，支持月度预算的创建、更新和监控")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(
        summary = "查询预算列表", 
        description = "获取预算列表，支持按年份和月份筛选，包含当前月份和历史的预算信息"
    )
    @OperationLog(value = "GET_BUDGETS", description = "获取预算列表", recordParams = false, recordResult = false)
    public ApiResponse<List<BudgetDto>> listBudgets(
            @Parameter(description = "年份，格式：yyyy") @RequestParam(value = "year", required = false) Integer year,
            @Parameter(description = "月份，1-12") @RequestParam(value = "month", required = false) Integer month) {
        return ApiResponse.success(budgetService.listBudgets(year, month));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取预算详情", 
        description = "根据ID获取单个预算的详细信息，包括预算金额、分类、月份和实际支出等完整信息"
    )
    @OperationLog(value = "GET_BUDGET", description = "获取预算详情", recordParams = false, recordResult = false)
    public ApiResponse<BudgetDto> getBudget(
            @Parameter(description = "预算ID，示例：1") @PathVariable Long id) {
        return ApiResponse.success(budgetService.getBudget(id));
    }

    @PostMapping
    @Operation(
        summary = "创建预算", 
        description = "创建新的预算设置，支持设置分类、预算金额、年份和月份"
    )
    @OperationLog(value = "CREATE_BUDGET", description = "创建预算", businessType = "BUDGET")
    public ApiResponse<BudgetDto> createBudget(
            @Parameter(description = "预算创建请求体，包含预算设置详细信息") @Valid @RequestBody CreateBudgetRequest request) {
        return ApiResponse.success(budgetService.createBudget(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新预算", 
        description = "更新指定ID的预算信息，支持修改预算金额和月份设置"
    )
    @OperationLog(value = "UPDATE_BUDGET", description = "更新预算", businessType = "BUDGET")
    public ApiResponse<BudgetDto> updateBudget(
            @Parameter(description = "预算ID，示例：1") @PathVariable Long id,
            @Parameter(description = "预算更新请求体") @Valid @RequestBody UpdateBudgetRequest request) {
        return ApiResponse.success(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除预算", 
        description = "删除指定ID的预算设置，删除后该分类的预算监控将失效"
    )
    @OperationLog(value = "DELETE_BUDGET", description = "删除预算", businessType = "BUDGET")
    public ApiResponse<Void> deleteBudget(
            @Parameter(description = "预算ID，示例：1") @PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ApiResponse.successMessage("Budget deleted");
    }
}
