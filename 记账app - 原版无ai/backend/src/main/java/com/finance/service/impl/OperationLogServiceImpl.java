package com.finance.service.impl;

import com.finance.entity.OperationLog;
import com.finance.repository.OperationLogRepository;
import com.finance.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现类
 */
@Service
@Transactional
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OperationLogServiceImpl(OperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void logOperation(String operationType, String description, Long userId) {
        OperationLog log = new OperationLog(operationType, description, userId);
        saveLog(log);
    }

    @Override
    public void logSuccess(String operationType, String description, Long userId, 
                          String operationResult, Long executionTime) {
        OperationLog log = new OperationLog(operationType, description, userId);
        log.markSuccess(operationResult, executionTime);
        saveLog(log);
    }

    @Override
    public void logFailure(String operationType, String description, Long userId,
                          String errorMessage, Long executionTime) {
        OperationLog log = new OperationLog(operationType, description, userId);
        log.markFailure(errorMessage, executionTime);
        saveLog(log);
    }

    @Override
    public void logPartial(String operationType, String description, Long userId,
                          String operationResult, String errorMessage, Long executionTime) {
        OperationLog log = new OperationLog(operationType, description, userId);
        log.markPartial(operationResult, errorMessage, executionTime);
        saveLog(log);
    }

    @Override
    public void logBusinessOperation(String operationType, String description, Long userId,
                                    String businessType, Long businessId) {
        OperationLog log = new OperationLog(operationType, description, userId);
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        saveLog(log);
    }

    @Override
    public void logDetailedOperation(String operationType, String description, Long userId,
                                    String operationParams, String ipAddress, String userAgent) {
        OperationLog log = new OperationLog(operationType, description, userId);
        log.setOperationParams(operationParams);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        saveLog(log);
    }

    @Override
    public Page<OperationLog> findByUserId(Long userId, Pageable pageable) {
        return operationLogRepository.findByUserIdOrderByOperationTimeDesc(userId, pageable);
    }

    @Override
    public Page<OperationLog> findByOperationType(String operationType, Pageable pageable) {
        return operationLogRepository.findByOperationTypeOrderByOperationTimeDesc(operationType, pageable);
    }

    @Override
    public Page<OperationLog> findByUserIdAndOperationType(Long userId, String operationType, Pageable pageable) {
        return operationLogRepository.findByUserIdAndOperationTypeOrderByOperationTimeDesc(userId, operationType, pageable);
    }

    @Override
    public Page<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.findByOperationTimeBetweenOrderByOperationTimeDesc(startTime, endTime, pageable);
    }

    @Override
    public Page<OperationLog> findByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.findByUserIdAndOperationTimeBetweenOrderByOperationTimeDesc(userId, startTime, endTime, pageable);
    }

    @Override
    public Page<OperationLog> findByStatus(String status, Pageable pageable) {
        return operationLogRepository.findByStatusOrderByOperationTimeDesc(status, pageable);
    }

    @Override
    public long countByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return operationLogRepository.countByUserIdAndOperationTimeBetween(userId, startTime, endTime);
    }

    @Override
    public List<OperationLog> findRecentFailures(Pageable pageable) {
        return operationLogRepository.findRecentFailures(pageable);
    }

    @Override
    public void cleanOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        operationLogRepository.deleteByOperationTimeBefore(cutoffDate);
        logger.info("清理完成：删除了{}之前的操作日志", cutoffDate);
    }

    /**
     * 保存操作日志
     */
    private void saveLog(OperationLog log) {
        try {
            operationLogRepository.save(log);
            logger.debug("操作日志已保存：类型={}, 用户={}, 描述={}", 
                        log.getOperationType(), log.getUserId(), log.getDescription());
        } catch (Exception e) {
            logger.error("保存操作日志失败：", e);
            // 不抛出异常，避免影响业务操作
        }
    }

    /**
     * 将对象转换为JSON字符串
     */
    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("JSON序列化失败：", e);
            return null;
        }
    }
}