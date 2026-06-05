package com.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class UserPreferencesRequest {
    @NotNull(message = "preferences不能为空")
    private Map<String, Object> preferences;
    
    // 可选：指定特定配置项的更新
    private String category; // budget, notification, display, etc.

    // Getter methods
    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public String getCategory() {
        return category;
    }

    // Setter methods
    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
