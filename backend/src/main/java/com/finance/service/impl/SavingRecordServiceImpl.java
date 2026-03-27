package com.finance.service.impl;

import com.finance.dto.CreateSavingRecordRequest;
import com.finance.dto.SavingRecordDto;
import com.finance.dto.UpdateSavingRecordRequest;
import com.finance.entity.SavingRecord;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.SavingRecordRepository;
import com.finance.service.SavingRecordService;
import com.finance.service.SavingGoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 储蓄记录服务实现类
 */
@Service
@Transactional
public class SavingRecordServiceImpl implements SavingRecordService {

    private static final Logger log = LoggerFactory.getLogger(SavingRecordServiceImpl.class);
    private final SavingRecordRepository savingRecordRepository;
    private final SavingGoalService savingGoalService;

    @Autowired
    public SavingRecordServiceImpl(SavingRecordRepository savingRecordRepository, SavingGoalService savingGoalService) {
        this.savingRecordRepository = savingRecordRepository;
        this.savingGoalService = savingGoalService;
    }

    @Override
    public SavingRecordDto createRecord(CreateSavingRecordRequest request) {
        log.info("创建储蓄记录: goalId={}, type={}, amount={}", 
                request.getGoalId(), request.getType(), request.getAmount());

        // 验证请求数据
        validateCreateRequest(request);

        // 验证储蓄目标是否存在
        if (!savingGoalService.existsById(request.getGoalId())) {
            throw new ResourceNotFoundException("储蓄目标不存在: " + request.getGoalId());
        }

        // 创建实体
        SavingRecord record = new SavingRecord();
        record.setGoalId(request.getGoalId());
        record.setType(request.getType().toUpperCase());
        record.setAmount(request.getAmount());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate());
        record.setCategory(request.getCategory());

        // 保存记录
        SavingRecord savedRecord = savingRecordRepository.save(record);
        log.info("储蓄记录创建成功: id={}", savedRecord.getId());

        return SavingRecordDto.fromEntity(savedRecord);
    }

    @Override
    public SavingRecordDto updateRecord(Long recordId, UpdateSavingRecordRequest request) {
        log.info("更新储蓄记录: id={}", recordId);

        // 获取现有记录
        SavingRecord record = savingRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("储蓄记录不存在: " + recordId));

        // 验证请求数据
        validateUpdateRequest(request);

        // 更新字段
        if (request.getAmount() != null) {
            record.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            record.setDescription(request.getDescription());
        }
        if (request.getRecordDate() != null) {
            record.setRecordDate(request.getRecordDate());
        }
        if (request.getCategory() != null) {
            record.setCategory(request.getCategory());
        }
        if (request.getType() != null) {
            record.setType(request.getType().toUpperCase());
        }

        // 保存更新
        SavingRecord updatedRecord = savingRecordRepository.save(record);
        log.info("储蓄记录更新成功: id={}", recordId);

        return SavingRecordDto.fromEntity(updatedRecord);
    }

    @Override
    public void deleteRecord(Long recordId) {
        log.info("删除储蓄记录: id={}", recordId);

        // 检查记录是否存在
        if (!savingRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("储蓄记录不存在: " + recordId);
        }

        // 删除记录
        savingRecordRepository.deleteById(recordId);
        log.info("储蓄记录删除成功: id={}", recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingRecordDto getRecordById(Long recordId) {
        log.debug("根据ID获取储蓄记录: id={}", recordId);

        SavingRecord record = savingRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("储蓄记录不存在: " + recordId));

        return SavingRecordDto.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingRecordDto> getRecordsByGoalId(Long goalId) {
        log.debug("根据储蓄目标ID获取记录列表: goalId={}", goalId);

        List<SavingRecord> records = savingRecordRepository.findByGoalId(goalId);
        return records.stream()
                .map(SavingRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SavingRecordDto> getRecordsByPage(Pageable pageable) {
        log.debug("分页查询储蓄记录: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<SavingRecord> records = savingRecordRepository.findAll(pageable);
        return records.map(SavingRecordDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingRecordDto> getRecordsByConditions(
            Long goalId, String type, LocalDateTime startDate, 
            LocalDateTime endDate, String category) {
        log.debug("根据条件查询储蓄记录: goalId={}, type={}, startDate={}, endDate={}, category={}",
                goalId, type, startDate, endDate, category);

        // 这里需要获取当前用户ID，暂时使用固定值
        Long currentUserId = 1L;
        
        List<SavingRecord> records = savingRecordRepository.findByConditions(
                currentUserId, goalId, type, startDate, endDate, category);
        
        return records.stream()
                .map(SavingRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalDepositByGoalId(Long goalId) {
        log.debug("获取储蓄目标总存款金额: goalId={}", goalId);

        return savingRecordRepository.sumDepositAmountByGoalId(goalId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalWithdrawByGoalId(Long goalId) {
        log.debug("获取储蓄目标总取款金额: goalId={}", goalId);

        return savingRecordRepository.sumWithdrawAmountByGoalId(goalId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getNetAmountByGoalId(Long goalId) {
        log.debug("获取储蓄目标净存款金额: goalId={}", goalId);

        BigDecimal deposit = getTotalDepositByGoalId(goalId);
        BigDecimal withdraw = getTotalWithdrawByGoalId(goalId);
        return deposit.subtract(withdraw);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingRecordStats getSavingRecordStats(Long userId) {
        log.debug("获取用户储蓄记录统计: userId={}", userId);

        BigDecimal totalDeposit = savingRecordRepository.sumDepositAmountByUserId(userId);
        BigDecimal totalWithdraw = savingRecordRepository.sumWithdrawAmountByUserId(userId);
        long totalRecords = savingRecordRepository.countByUserId(userId);
        long depositCount = savingRecordRepository.countDepositByUserId(userId);
        long withdrawCount = savingRecordRepository.countWithdrawByUserId(userId);

        return new SavingRecordStatsImpl(
                totalDeposit, totalWithdraw, totalRecords, depositCount, withdrawCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long recordId) {
        return savingRecordRepository.existsById(recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Long recordId, Long userId) {
        return savingRecordRepository.existsByIdAndUserId(recordId, userId);
    }

    /**
     * 验证创建请求数据
     */
    private void validateCreateRequest(CreateSavingRecordRequest request) {
        if (!request.isValidType()) {
            throw new IllegalArgumentException("无效的记录类型: " + request.getType());
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (request.getRecordDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("记录日期不能超过当前时间");
        }
    }

    /**
     * 验证更新请求数据
     */
    private void validateUpdateRequest(UpdateSavingRecordRequest request) {
        if (request.getType() != null && !request.isValidType()) {
            throw new IllegalArgumentException("无效的记录类型: " + request.getType());
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (request.getRecordDate() != null && request.getRecordDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("记录日期不能超过当前时间");
        }
    }

    /**
     * 储蓄记录统计信息实现类
     */
    private static class SavingRecordStatsImpl implements SavingRecordStats {
        private final BigDecimal totalDeposit;
        private final BigDecimal totalWithdraw;
        private final long totalRecords;
        private final long depositCount;
        private final long withdrawCount;

        // Constructor
        public SavingRecordStatsImpl(BigDecimal totalDeposit, BigDecimal totalWithdraw, long totalRecords, long depositCount, long withdrawCount) {
            this.totalDeposit = totalDeposit;
            this.totalWithdraw = totalWithdraw;
            this.totalRecords = totalRecords;
            this.depositCount = depositCount;
            this.withdrawCount = withdrawCount;
        }

        @Override
        public BigDecimal getTotalDeposit() {
            return totalDeposit;
        }

        @Override
        public BigDecimal getTotalWithdraw() {
            return totalWithdraw;
        }

        @Override
        public BigDecimal getNetAmount() {
            return totalDeposit.subtract(totalWithdraw);
        }

        @Override
        public int getTotalRecords() {
            return (int) totalRecords;
        }

        @Override
        public int getDepositCount() {
            return (int) depositCount;
        }

        @Override
        public int getWithdrawCount() {
            return (int) withdrawCount;
        }
    }
}