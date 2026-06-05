package com.campus.trade.dto.admin;

import com.campus.trade.model.enums.SystemConfigScope;
import com.campus.trade.model.enums.SystemConfigStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SystemConfigCreateRequest {

    @NotBlank
    private String key;

    @NotNull
    private String value;

    private String description;

    private SystemConfigScope scope;

    private SystemConfigStatus status;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SystemConfigScope getScope() {
        return scope;
    }

    public void setScope(SystemConfigScope scope) {
        this.scope = scope;
    }

    public SystemConfigStatus getStatus() {
        return status;
    }

    public void setStatus(SystemConfigStatus status) {
        this.status = status;
    }
}
