package com.finance.service;

import com.finance.dto.CreateSavingGoalRequest;
import com.finance.dto.SavingGoalDto;
import com.finance.dto.UpdateSavingGoalAmountRequest;
import com.finance.dto.UpdateSavingGoalRequest;

import java.math.BigDecimal;
import java.util.List;

public interface SavingGoalService {

    List<SavingGoalDto> listSavingGoals();

    SavingGoalDto getSavingGoal(Long id);

    SavingGoalDto createSavingGoal(CreateSavingGoalRequest request);

    SavingGoalDto updateSavingGoal(Long id, UpdateSavingGoalRequest request);

    SavingGoalDto updateCurrentAmount(Long id, UpdateSavingGoalAmountRequest request);

    void deleteSavingGoal(Long id);

    /**
     * 计算储蓄目标的进度百分比
     */
    BigDecimal calculateProgressPercentage(Long goalId);

    /**
     * 获取储蓄目标的剩余金额
     */
    BigDecimal getRemainingAmount(Long goalId);

    /**
     * 获取储蓄目标的剩余天数
     */
    long getRemainingDays(Long goalId);

    /**
     * 计算每日应存金额
     */
    BigDecimal getDailySavingAmount(Long goalId);

    /**
     * 检查储蓄目标是否已完成
     */
    boolean isGoalCompleted(Long goalId);

    /**
     * 检查储蓄目标是否存在
     */
    boolean existsById(Long goalId);

    /**
     * 获取用户所有储蓄目标的统计信息
     */
    SavingGoalStats getSavingGoalStats();

    /**
     * 储蓄目标统计信息
     */
    interface SavingGoalStats {
        BigDecimal getTotalTargetAmount();
        BigDecimal getTotalCurrentAmount();
        BigDecimal getTotalProgressPercentage();
        BigDecimal getTotalRemainingAmount();
        int getTotalGoals();
        int getCompletedGoals();
        int getActiveGoals();
        int getOverdueGoals();
    }
}
