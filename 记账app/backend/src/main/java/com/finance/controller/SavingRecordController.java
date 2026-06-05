package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.CreateSavingRecordRequest;
import com.finance.dto.SavingRecordDto;
import com.finance.dto.UpdateSavingRecordRequest;
import com.finance.service.SavingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 储蓄记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/saving-records")
@RequiredArgsConstructor
public class SavingRecordController {

    // 显式添加 log 变量，解决 Lombok 可能的注解处理问题
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SavingRecordController.class);
    
    private final SavingRecordService savingRecordService;

    /**
     * 创建储蓄记录
     */
    @PostMapping
    @OperationLog(value = "CREATE_SAVING_RECORD", description = "创建储蓄记录", businessType = "SAVING_RECORD")
    public ResponseEntity<SavingRecordDto> createRecord(@Valid @RequestBody CreateSavingRecordRequest request) {
        log.info("创建储蓄记录请求: goalId={}, type={}, amount={}", 
                request.getGoalId(), request.getType(), request.getAmount());
        
        SavingRecordDto record = savingRecordService.createRecord(request);
        return ResponseEntity.ok(record);
    }

    /**
     * 更新储蓄记录
     */
    @PutMapping("/{id}")
    @OperationLog(value = "UPDATE_SAVING_RECORD", description = "更新储蓄记录", businessType = "SAVING_RECORD")
    public ResponseEntity<SavingRecordDto> updateRecord(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateSavingRecordRequest request) {
        log.info("更新储蓄记录请求: id={}", id);
        
        SavingRecordDto record = savingRecordService.updateRecord(id, request);
        return ResponseEntity.ok(record);
    }

    /**
     * 删除储蓄记录
     */
    @DeleteMapping("/{id}")
    @OperationLog(value = "DELETE_SAVING_RECORD", description = "删除储蓄记录", businessType = "SAVING_RECORD")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        log.info("删除储蓄记录请求: id={}", id);
        
        savingRecordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据ID获取储蓄记录
     */
    @GetMapping("/{id}")
    @OperationLog(value = "GET_SAVING_RECORD", description = "获取储蓄记录详情", recordParams = false, recordResult = false)
    public ResponseEntity<SavingRecordDto> getRecordById(@PathVariable Long id) {
        log.debug("获取储蓄记录请求: id={}", id);
        
        SavingRecordDto record = savingRecordService.getRecordById(id);
        return ResponseEntity.ok(record);
    }

    /**
     * 根据储蓄目标ID获取记录列表
     */
    @GetMapping("/goal/{goalId}")
    @OperationLog(value = "GET_SAVING_RECORDS_BY_GOAL", description = "获取储蓄目标记录列表", recordParams = false, recordResult = false)
    public ResponseEntity<List<SavingRecordDto>> getRecordsByGoalId(@PathVariable Long goalId) {
        log.debug("根据储蓄目标ID获取记录列表请求: goalId={}", goalId);
        
        List<SavingRecordDto> records = savingRecordService.getRecordsByGoalId(goalId);
        return ResponseEntity.ok(records);
    }

    /**
     * 分页查询储蓄记录
     */
    @GetMapping
    @OperationLog(value = "GET_SAVING_RECORDS_PAGED", description = "分页查询储蓄记录", recordParams = false, recordResult = false)
    public ResponseEntity<Page<SavingRecordDto>> getRecordsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        log.debug("分页查询储蓄记录请求: page={}, size={}, sort={}, direction={}", 
                page, size, sort, direction);
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<SavingRecordDto> records = savingRecordService.getRecordsByPage(pageable);
        return ResponseEntity.ok(records);
    }

    /**
     * 根据条件查询储蓄记录
     */
    @GetMapping("/search")
    @OperationLog(value = "SEARCH_SAVING_RECORDS", description = "条件查询储蓄记录", recordParams = false, recordResult = false)
    public ResponseEntity<List<SavingRecordDto>> searchRecords(
            @RequestParam(required = false) Long goalId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) String category) {
        
        log.debug("条件查询储蓄记录请求: goalId={}, type={}, startDate={}, endDate={}, category={}",
                goalId, type, startDate, endDate, category);
        
        List<SavingRecordDto> records = savingRecordService.getRecordsByConditions(
                goalId, type, startDate, endDate, category);
        return ResponseEntity.ok(records);
    }

    /**
     * 获取储蓄目标的总存款金额
     */
    @GetMapping("/goal/{goalId}/total-deposit")
    @OperationLog(value = "GET_TOTAL_DEPOSIT", description = "获取目标总存款金额", recordParams = false, recordResult = false)
    public ResponseEntity<BigDecimal> getTotalDepositByGoalId(@PathVariable Long goalId) {
        log.debug("获取储蓄目标总存款金额请求: goalId={}", goalId);
        
        BigDecimal totalDeposit = savingRecordService.getTotalDepositByGoalId(goalId);
        return ResponseEntity.ok(totalDeposit);
    }

    /**
     * 获取储蓄目标的总取款金额
     */
    @GetMapping("/goal/{goalId}/total-withdraw")
    @OperationLog(value = "GET_TOTAL_WITHDRAW", description = "获取目标总取款金额", recordParams = false, recordResult = false)
    public ResponseEntity<BigDecimal> getTotalWithdrawByGoalId(@PathVariable Long goalId) {
        log.debug("获取储蓄目标总取款金额请求: goalId={}", goalId);
        
        BigDecimal totalWithdraw = savingRecordService.getTotalWithdrawByGoalId(goalId);
        return ResponseEntity.ok(totalWithdraw);
    }

    /**
     * 获取储蓄目标的净存款金额
     */
    @GetMapping("/goal/{goalId}/net-amount")
    @OperationLog(value = "GET_NET_AMOUNT", description = "获取目标净存款金额", recordParams = false, recordResult = false)
    public ResponseEntity<BigDecimal> getNetAmountByGoalId(@PathVariable Long goalId) {
        log.debug("获取储蓄目标净存款金额请求: goalId={}", goalId);
        
        BigDecimal netAmount = savingRecordService.getNetAmountByGoalId(goalId);
        return ResponseEntity.ok(netAmount);
    }

    /**
     * 获取用户储蓄记录统计
     */
    @GetMapping("/stats")
    @OperationLog(value = "GET_SAVING_RECORD_STATS", description = "获取用户储蓄记录统计", recordParams = false, recordResult = false)
    public ResponseEntity<SavingRecordService.SavingRecordStats> getSavingRecordStats(
            @RequestParam Long userId) {
        log.debug("获取用户储蓄记录统计请求: userId={}", userId);
        
        SavingRecordService.SavingRecordStats stats = savingRecordService.getSavingRecordStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 检查记录是否存在
     */
    @GetMapping("/{id}/exists")
    @OperationLog(value = "CHECK_SAVING_RECORD_EXISTS", description = "检查储蓄记录是否存在", recordParams = false, recordResult = false)
    public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
        log.debug("检查储蓄记录是否存在请求: id={}", id);
        
        boolean exists = savingRecordService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    /**
     * 检查用户是否有权限操作记录
     */
    @GetMapping("/{id}/permission")
    @OperationLog(value = "CHECK_SAVING_RECORD_PERMISSION", description = "检查用户操作权限", recordParams = false, recordResult = false)
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable Long id, 
            @RequestParam Long userId) {
        log.debug("检查用户权限请求: recordId={}, userId={}", id, userId);
        
        boolean hasPermission = savingRecordService.hasPermission(id, userId);
        return ResponseEntity.ok(hasPermission);
    }

    /**
     * 批量删除储蓄记录
     */
    @DeleteMapping("/batch")
    @OperationLog(value = "BATCH_DELETE_SAVING_RECORD", description = "批量删除储蓄记录", businessType = "SAVING_RECORD")
    public ResponseEntity<Void> batchDeleteRecords(@RequestBody List<Long> recordIds) {
        log.info("批量删除储蓄记录请求: recordIds={}", recordIds);
        
        for (Long recordId : recordIds) {
            savingRecordService.deleteRecord(recordId);
        }
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.debug("储蓄记录服务健康检查");
        return ResponseEntity.ok("Saving Record Service is healthy");
    }
}