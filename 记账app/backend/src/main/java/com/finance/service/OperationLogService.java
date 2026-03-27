package com.finance.service;

import com.finance.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void logOperation(String operationType, String description, Long userId);

    /**
     * 记录成功操作
     */
    void logSuccess(String operationType, String description, Long userId, 
                    String operationResult, Long executionTime);

    /**
     * 记录失败操作
     */
    void logFailure(String operationType, String description, Long userId,
                    String errorMessage, Long executionTime);

    /**
     * 记录部分成功操作
     */
    void logPartial(String operationType, String description, Long userId,
                    String operationResult, String errorMessage, Long executionTime);

    /**
     * 记录带业务对象的操作
     */
    void logBusinessOperation(String operationType, String description, Long userId,
                             String businessType, Long businessId);

    /**
     * 记录带额外信息的操作
     */
    void logDetailedOperation(String operationType, String description, Long userId,
                             String operationParams, String ipAddress, String userAgent);

    /**
     * 根据用户ID分页查询操作日志
     */
    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据操作类型分页查询操作日志
     */
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    /**
     * 根据用户ID和操作类型分页查询操作日志
     */
    Page<OperationLog> findByUserIdAndOperationType(Long userId, String operationType, Pageable pageable);

    /**
     * 根据时间范围分页查询操作日志
     */
    Page<OperationLog> findByTimeRange(java.time.LocalDateTime startTime, 
                                      java.time.LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户ID和时间范围分页查询操作日志
     */
    Page<OperationLog> findByUserIdAndTimeRange(Long userId, 
                                               java.time.LocalDateTime startTime,
                                               java.time.LocalDateTime endTime, Pageable pageable);

    /**
     * 根据状态分页查询操作日志
     */
    Page<OperationLog> findByStatus(String status, Pageable pageable);

    /**
     * 统计用户在指定时间范围内的操作次数
     */
    long countByUserIdAndTimeRange(Long userId, java.time.LocalDateTime startTime, 
                                  java.time.LocalDateTime endTime);

    /**
     * 获取最近的错误操作
     */
    java.util.List<OperationLog> findRecentFailures(Pageable pageable);

    /**
     * 清理过期日志
     */
    void cleanOldLogs(int daysToKeep);
}