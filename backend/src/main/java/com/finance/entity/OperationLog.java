package com.finance.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 记录系统中所有关键业务操作的审计日志
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "operation_logs",
    indexes = {
        @Index(name = "idx_operation_logs_user", columnList = "user_id"),
        @Index(name = "idx_operation_logs_operation_time", columnList = "operation_time"),
        @Index(name = "idx_operation_logs_operation_type", columnList = "operation_type"),
        @Index(name = "idx_operation_logs_user_time", columnList = "user_id,operation_time")
    })
public class OperationLog extends UserScopedAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作类型
     * 例如：CREATE_TRANSACTION, UPDATE_BUDGET, DELETE_CATEGORY等
     */
    @Column(nullable = false, length = 50)
    private String operationType;

    /**
     * 操作描述
     */
    @Column(length = 200)
    private String description;

    /**
     * 操作参数（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String operationParams;

    /**
     * 操作结果（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String operationResult;

    /**
     * 操作状态：SUCCESS=成功, FAILURE=失败, PARTIAL=部分成功
     */
    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    /**
     * 错误信息（如果操作失败）
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 操作耗时（毫秒）
     */
    @Column
    private Long executionTime;

    /**
     * IP地址
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * 用户代理
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * 操作时间（覆盖审计基类的时间字段语义）
     */
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    /**
     * 业务对象ID（如果操作涉及具体的业务对象）
     */
    @Column(name = "business_id")
    private Long businessId;

    /**
     * 业务对象类型
     */
    @Column(name = "business_type", length = 50)
    private String businessType;

    public OperationLog(String operationType, String description, Long userId) {
        this.operationType = operationType;
        this.description = description;
        this.setUserId(userId);
        this.operationTime = LocalDateTime.now();
    }

    /**
     * 标记操作成功完成
     */
    public void markSuccess(String operationResult, Long executionTime) {
        this.status = "SUCCESS";
        this.operationResult = operationResult;
        this.executionTime = executionTime;
    }

    /**
     * 标记操作失败
     */
    public void markFailure(String errorMessage, Long executionTime) {
        this.status = "FAILURE";
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
    }

    /**
     * 标记操作部分成功
     */
    public void markPartial(String operationResult, String errorMessage, Long executionTime) {
        this.status = "PARTIAL";
        this.operationResult = operationResult;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationParams() {
        return operationParams;
    }

    public void setOperationParams(String operationParams) {
        this.operationParams = operationParams;
    }

    public String getOperationResult() {
        return operationResult;
    }

    public void setOperationResult(String operationResult) {
        this.operationResult = operationResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}