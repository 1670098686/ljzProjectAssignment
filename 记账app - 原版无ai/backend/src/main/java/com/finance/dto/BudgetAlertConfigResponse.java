package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BudgetAlertConfigResponse {
    private Long configId;
    private BigDecimal warningThreshold;
    private BigDecimal criticalThreshold;
    private Boolean pushEnabled;
    private Boolean enableAllCategories;
    private List<Long> categoryIds;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Constructor
    public BudgetAlertConfigResponse() {
        // Default constructor
    }

    public BudgetAlertConfigResponse(Long configId, BigDecimal warningThreshold, BigDecimal criticalThreshold, 
                                    Boolean pushEnabled, Boolean enableAllCategories, List<Long> categoryIds, 
                                    LocalDateTime createdTime, LocalDateTime updatedTime) {
        this.configId = configId;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.pushEnabled = pushEnabled;
        this.enableAllCategories = enableAllCategories;
        this.categoryIds = categoryIds;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    // Getter methods
    public Long getConfigId() {
        return configId;
    }

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

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    // Setter methods
    public void setConfigId(Long configId) {
        this.configId = configId;
    }

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

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long configId;
        private BigDecimal warningThreshold;
        private BigDecimal criticalThreshold;
        private Boolean pushEnabled;
        private Boolean enableAllCategories;
        private List<Long> categoryIds;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;

        public Builder configId(Long configId) {
            this.configId = configId;
            return this;
        }

        public Builder warningThreshold(BigDecimal warningThreshold) {
            this.warningThreshold = warningThreshold;
            return this;
        }

        public Builder criticalThreshold(BigDecimal criticalThreshold) {
            this.criticalThreshold = criticalThreshold;
            return this;
        }

        public Builder pushEnabled(Boolean pushEnabled) {
            this.pushEnabled = pushEnabled;
            return this;
        }

        public Builder enableAllCategories(Boolean enableAllCategories) {
            this.enableAllCategories = enableAllCategories;
            return this;
        }

        public Builder categoryIds(List<Long> categoryIds) {
            this.categoryIds = categoryIds;
            return this;
        }

        public Builder createdTime(LocalDateTime createdTime) {
            this.createdTime = createdTime;
            return this;
        }

        public Builder updatedTime(LocalDateTime updatedTime) {
            this.updatedTime = updatedTime;
            return this;
        }

        public BudgetAlertConfigResponse build() {
            return new BudgetAlertConfigResponse(
                configId,
                warningThreshold,
                criticalThreshold,
                pushEnabled,
                enableAllCategories,
                categoryIds,
                createdTime,
                updatedTime
            );
        }
    }
}