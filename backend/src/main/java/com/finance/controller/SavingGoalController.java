package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.CreateSavingGoalRequest;
import com.finance.dto.SavingGoalDto;
import com.finance.dto.UpdateSavingGoalAmountRequest;
import com.finance.dto.UpdateSavingGoalRequest;
import com.finance.service.SavingGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saving-goals")
@Tag(name = "储蓄目标管理", description = "储蓄目标设置和查询接口，支持储蓄目标的创建、进度跟踪和管理")
public class SavingGoalController {

    private final SavingGoalService savingGoalService;

    public SavingGoalController(SavingGoalService savingGoalService) {
        this.savingGoalService = savingGoalService;
    }

    @GetMapping
    @Operation(
        summary = "获取储蓄目标列表", 
        description = "获取所有储蓄目标，包含已完成和进行中的目标，按截止日期排序"
    )
    @OperationLog(value = "GET_SAVING_GOALS", description = "获取储蓄目标列表", recordParams = false, recordResult = false)
    public ApiResponse<List<SavingGoalDto>> listSavingGoals() {
        return ApiResponse.success(savingGoalService.listSavingGoals());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取储蓄目标详情", 
        description = "根据ID获取储蓄目标详细信息，包括目标金额、当前进度、截止日期和完成状态"
    )
    @OperationLog(value = "GET_SAVING_GOAL", description = "获取储蓄目标详情", recordParams = false, recordResult = false)
    public ApiResponse<SavingGoalDto> getSavingGoal(
            @Parameter(description = "储蓄目标ID，示例：1") @PathVariable Long id) {
        return ApiResponse.success(savingGoalService.getSavingGoal(id));
    }

    @PostMapping
    @Operation(
        summary = "创建储蓄目标", 
        description = "创建新的储蓄目标，支持设置目标名称、目标金额、截止日期和描述信息"
    )
    @OperationLog(value = "CREATE_SAVING_GOAL", description = "创建储蓄目标", businessType = "SAVING_GOAL")
    public ApiResponse<SavingGoalDto> createSavingGoal(
            @Parameter(description = "储蓄目标创建请求体，包含目标设置详细信息") @Valid @RequestBody CreateSavingGoalRequest request) {
        return ApiResponse.success(savingGoalService.createSavingGoal(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新储蓄目标", 
        description = "更新指定ID的储蓄目标信息，支持修改目标金额、截止日期和描述信息"
    )
    @OperationLog(value = "UPDATE_SAVING_GOAL", description = "更新储蓄目标", businessType = "SAVING_GOAL")
    public ApiResponse<SavingGoalDto> updateSavingGoal(
            @Parameter(description = "储蓄目标ID，示例：1") @PathVariable Long id,
            @Parameter(description = "储蓄目标更新请求体") @Valid @RequestBody UpdateSavingGoalRequest request) {
        return ApiResponse.success(savingGoalService.updateSavingGoal(id, request));
    }

    @PatchMapping("/{id}/current-amount")
    @Operation(summary = "更新当前金额", description = "更新指定储蓄目标的当前金额")
    @OperationLog(value = "UPDATE_SAVING_GOAL_CURRENT_AMOUNT", description = "更新储蓄目标当前金额", businessType = "SAVING_GOAL")
    public ApiResponse<SavingGoalDto> updateCurrentAmount(
            @Parameter(description = "储蓄目标ID") @PathVariable Long id,
            @Parameter(description = "当前金额更新请求") @Valid @RequestBody UpdateSavingGoalAmountRequest request) {
        return ApiResponse.success(savingGoalService.updateCurrentAmount(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除储蓄目标", 
        description = "删除指定ID的储蓄目标，删除后目标进度信息将丢失"
    )
    @OperationLog(value = "DELETE_SAVING_GOAL", description = "删除储蓄目标", businessType = "SAVING_GOAL")
    public ApiResponse<Void> deleteSavingGoal(
            @Parameter(description = "储蓄目标ID，示例：1") @PathVariable Long id) {
        savingGoalService.deleteSavingGoal(id);
        return ApiResponse.successMessage("Saving goal deleted");
    }
}
