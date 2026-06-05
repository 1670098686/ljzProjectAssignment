package com.finance.controller;

import com.finance.entity.OperationLog;
import com.finance.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志管理控制器
 */
@Tag(name = "操作日志管理", description = "操作日志查询和管理接口")
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Autowired
    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 根据用户ID分页查询操作日志
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询操作日志", description = "分页查询指定用户的操作日志")
    @com.finance.annotation.OperationLog(value = "GET_USER_OPERATION_LOGS", description = "根据用户ID查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getUserOperationLogs(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByUserId(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据操作类型分页查询操作日志
     */
    @GetMapping("/type/{operationType}")
    @Operation(summary = "根据操作类型查询操作日志", description = "分页查询指定类型的操作日志")
    @com.finance.annotation.OperationLog(value = "GET_OPERATION_LOGS_BY_TYPE", description = "根据操作类型查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getOperationLogsByType(
            @Parameter(description = "操作类型", required = true)
            @PathVariable String operationType,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByOperationType(operationType, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据用户ID和操作类型分页查询操作日志
     */
    @GetMapping("/user/{userId}/type/{operationType}")
    @Operation(summary = "根据用户ID和操作类型查询操作日志", description = "分页查询指定用户的指定类型操作日志")
    @com.finance.annotation.OperationLog(value = "GET_USER_OPERATION_LOGS_BY_TYPE", description = "根据用户ID和操作类型查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getUserOperationLogsByType(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "操作类型", required = true)
            @PathVariable String operationType,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByUserIdAndOperationType(userId, operationType, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据时间范围分页查询操作日志
     */
    @GetMapping("/time-range")
    @Operation(summary = "根据时间范围查询操作日志", description = "分页查询指定时间范围内的操作日志")
    @com.finance.annotation.OperationLog(value = "GET_OPERATION_LOGS_BY_TIME_RANGE", description = "根据时间范围查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getOperationLogsByTimeRange(
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByTimeRange(startTime, endTime, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据用户ID和时间范围分页查询操作日志
     */
    @GetMapping("/user/{userId}/time-range")
    @Operation(summary = "根据用户ID和时间范围查询操作日志", description = "分页查询指定用户在指定时间范围内的操作日志")
    @com.finance.annotation.OperationLog(value = "GET_USER_OPERATION_LOGS_BY_TIME_RANGE", description = "根据用户ID和时间范围查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getUserOperationLogsByTimeRange(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByUserIdAndTimeRange(userId, startTime, endTime, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据状态分页查询操作日志
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态查询操作日志", description = "分页查询指定状态的操阤日志（SUCCESS、FAILURE、PARTIAL）")
    @com.finance.annotation.OperationLog(value = "GET_OPERATION_LOGS_BY_STATUS", description = "根据状态查询操作日志", recordParams = false, recordResult = false)
    public ResponseEntity<Page<OperationLog>> getOperationLogsByStatus(
            @Parameter(description = "操作状态", required = true)
            @PathVariable String status,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OperationLog> logs = operationLogService.findByStatus(status, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 统计用户在指定时间范围内的操作次数
     */
    @GetMapping("/user/{userId}/count")
    @Operation(summary = "统计用户操作次数", description = "统计指定用户在指定时间范围内的操作次数")
    @com.finance.annotation.OperationLog(value = "COUNT_USER_OPERATIONS", description = "统计用户操作次数", recordParams = false, recordResult = false)
    public ResponseEntity<Long> countUserOperations(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        long count = operationLogService.countByUserIdAndTimeRange(userId, startTime, endTime);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取最近的错误操作
     */
    @GetMapping("/recent-failures")
    @Operation(summary = "获取最近的错误操作", description = "查询最近的失败操作日志")
    @com.finance.annotation.OperationLog(value = "GET_RECENT_FAILURES", description = "获取最近的错误操作", recordParams = false, recordResult = false)
    public ResponseEntity<List<OperationLog>> getRecentFailures(
            @Parameter(description = "返回记录数量")
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(0, size);
        List<OperationLog> logs = operationLogService.findRecentFailures(pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * 清理过期日志
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期日志", description = "清理超过指定天数的操作日志")
    @com.finance.annotation.OperationLog(value = "CLEANUP_OLD_LOGS", description = "清理过期日志", businessType = "OPERATION_LOG")
    public ResponseEntity<String> cleanupOldLogs(
            @Parameter(description = "保留天数", required = true)
            @RequestParam int daysToKeep) {
        
        operationLogService.cleanOldLogs(daysToKeep);
        return ResponseEntity.ok("已清理" + daysToKeep + "天之前的操作日志");
    }
}