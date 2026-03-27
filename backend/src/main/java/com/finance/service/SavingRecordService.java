package com.finance.service;

import com.finance.dto.CreateSavingRecordRequest;
import com.finance.dto.SavingRecordDto;
import com.finance.dto.UpdateSavingRecordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 储蓄记录服务接口
 */
public interface SavingRecordService {

    /**
     * 创建储蓄记录
     */
    SavingRecordDto createRecord(CreateSavingRecordRequest request);

    /**
     * 更新储蓄记录
     */
    SavingRecordDto updateRecord(Long recordId, UpdateSavingRecordRequest request);

    /**
     * 删除储蓄记录
     */
    void deleteRecord(Long recordId);

    /**
     * 根据ID获取储蓄记录
     */
    SavingRecordDto getRecordById(Long recordId);

    /**
     * 根据储蓄目标ID获取记录列表
     */
    List<SavingRecordDto> getRecordsByGoalId(Long goalId);

    /**
     * 分页查询储蓄记录
     */
    Page<SavingRecordDto> getRecordsByPage(Pageable pageable);

    /**
     * 根据条件查询储蓄记录
     */
    List<SavingRecordDto> getRecordsByConditions(
        Long goalId,
        String type,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String category
    );

    /**
     * 获取储蓄目标的总存款金额
     */
    BigDecimal getTotalDepositByGoalId(Long goalId);

    /**
     * 获取储蓄目标的总取款金额
     */
    BigDecimal getTotalWithdrawByGoalId(Long goalId);

    /**
     * 获取储蓄目标的净存款金额（存款 - 取款）
     */
    BigDecimal getNetAmountByGoalId(Long goalId);

    /**
     * 获取用户的所有储蓄记录统计
     */
    SavingRecordStats getSavingRecordStats(Long userId);

    /**
     * 验证储蓄记录是否存在
     */
    boolean existsById(Long recordId);

    /**
     * 验证用户是否有权限操作该记录
     */
    boolean hasPermission(Long recordId, Long userId);

    /**
     * 储蓄记录统计信息
     */
    interface SavingRecordStats {
        BigDecimal getTotalDeposit();
        BigDecimal getTotalWithdraw();
        BigDecimal getNetAmount();
        int getTotalRecords();
        int getDepositCount();
        int getWithdrawCount();
    }
}