package com.finance.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class UserPreferencesResponse {
    private Boolean success;
    private String message;
    private Map<String, Object> preferences;
    private String category; // 更新的配置类别
    private LocalDateTime updatedTime;

    // Getter methods
    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    // Setter methods
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}