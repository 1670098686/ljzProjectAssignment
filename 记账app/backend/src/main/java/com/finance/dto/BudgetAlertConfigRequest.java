package com.finance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetAlertConfigRequest {
    
    @NotNull(message = "warningThreshold不能为空")
    @DecimalMin(value = "0.01", message = "warningThreshold必须大于0")
    @DecimalMax(value = "0.99", message = "warningThreshold必须小于1")
    private BigDecimal warningThreshold;
    
    @NotNull(message = "criticalThreshold不能为空")
    @DecimalMin(value = "0.01", message = "criticalThreshold必须大于0")
    @DecimalMax(value = "0.99", message = "criticalThreshold必须小于1")
    private BigDecimal criticalThreshold;
    
    @NotNull(message = "pushEnabled不能为空")
    private Boolean pushEnabled;
    
    @NotNull(message = "enableAllCategories不能为空")
    private Boolean enableAllCategories;
    
    private java.util.List<Long> categoryIds; // 当enableAllCategories为false时，指定分类列表

    // Getter methods
    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public BigDecimal getCriticalThreshold() {
        return criticalThreshold;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getEnableAllCategories() {
        return enableAllCategories;
    }

    public java.util.List<Long> getCategoryIds() {
        return categoryIds;
    }

    // Setter methods
    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public void setCriticalThreshold(BigDecimal criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public void setEnableAllCategories(Boolean enableAllCategories) {
        this.enableAllCategories = enableAllCategories;
    }

    public void setCategoryIds(java.util.List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}