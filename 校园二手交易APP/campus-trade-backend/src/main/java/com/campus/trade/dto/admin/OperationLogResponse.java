package com.campus.trade.dto.admin;

import com.campus.trade.model.entity.OperationLogEntry;
import com.campus.trade.model.enums.OperationResult;
import com.campus.trade.model.enums.OperationType;
import java.time.LocalDateTime;

public class OperationLogResponse {

    private Long id;
    private String operator;
    private String ip;
    private String endpoint;
    private String httpMethod;
    private String title;
    private String action;
    private OperationType type;
    private OperationResult result;
    private String errorMessage;
    private String resourceId;
    private LocalDateTime createTime;

    public static OperationLogResponse from(OperationLogEntry entry) {
        OperationLogResponse response = new OperationLogResponse();
        response.setId(entry.getId());
        response.setOperator(entry.getOperator());
        response.setIp(entry.getIp());
        response.setEndpoint(entry.getEndpoint());
        response.setHttpMethod(entry.getHttpMethod());
        response.setTitle(entry.getTitle());
        response.setAction(entry.getAction());
        response.setType(entry.getType());
        response.setResult(entry.getResult());
        response.setErrorMessage(entry.getErrorMessage());
        response.setResourceId(entry.getResourceId());
        response.setCreateTime(entry.getCreateTime());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
