package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BudgetAlertDto {
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private Integer year;
    private Integer month;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usageRate;
    private String alertLevel;
    private BigDecimal triggeredThreshold;
    private String message;
    private boolean notificationSent;
    private LocalDateTime alertTime;

    // Constructor
    public BudgetAlertDto() {
        // Default constructor
    }

    public BudgetAlertDto(Long budgetId, Long categoryId, String categoryName, Integer year, Integer month, 
                         BigDecimal budgetAmount, BigDecimal spentAmount, BigDecimal remainingAmount, 
                         BigDecimal usageRate, String alertLevel, BigDecimal triggeredThreshold, 
                         String message, boolean notificationSent, LocalDateTime alertTime) {
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.year = year;
        this.month = month;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.usageRate = usageRate;
        this.alertLevel = alertLevel;
        this.triggeredThreshold = triggeredThreshold;
        this.message = message;
        this.notificationSent = notificationSent;
        this.alertTime = alertTime;
    }

    // Getter methods
    public Long getBudgetId() {
        return budgetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public BigDecimal getUsageRate() {
        return usageRate;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public BigDecimal getTriggeredThreshold() {
        return triggeredThreshold;
    }

    public String getMessage() {
        return message;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }
    
    // 兼容方法：返回使用率百分比（与usageRate相同）
    public BigDecimal getUsagePercentage() {
        return usageRate;
    }

    // Setter methods
    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public void setUsageRate(BigDecimal usageRate) {
        this.usageRate = usageRate;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public void setTriggeredThreshold(BigDecimal triggeredThreshold) {
        this.triggeredThreshold = triggeredThreshold;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long budgetId;
        private Long categoryId;
        private String categoryName;
        private Integer year;
        private Integer month;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private BigDecimal usageRate;
        private String alertLevel;
        private BigDecimal triggeredThreshold;
        private String message;
        private boolean notificationSent;
        private LocalDateTime alertTime;

        public Builder budgetId(Long budgetId) {
            this.budgetId = budgetId;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder year(Integer year) {
            this.year = year;
            return this;
        }

        public Builder month(Integer month) {
            this.month = month;
            return this;
        }

        public Builder budgetAmount(BigDecimal budgetAmount) {
            this.budgetAmount = budgetAmount;
            return this;
        }

        public Builder spentAmount(BigDecimal spentAmount) {
            this.spentAmount = spentAmount;
            return this;
        }

        public Builder remainingAmount(BigDecimal remainingAmount) {
            this.remainingAmount = remainingAmount;
            return this;
        }

        public Builder usageRate(BigDecimal usageRate) {
            this.usageRate = usageRate;
            return this;
        }

        public Builder alertLevel(String alertLevel) {
            this.alertLevel = alertLevel;
            return this;
        }

        public Builder triggeredThreshold(BigDecimal triggeredThreshold) {
            this.triggeredThreshold = triggeredThreshold;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder notificationSent(boolean notificationSent) {
            this.notificationSent = notificationSent;
            return this;
        }

        public Builder alertTime(LocalDateTime alertTime) {
            this.alertTime = alertTime;
            return this;
        }

        public BudgetAlertDto build() {
            return new BudgetAlertDto(
                budgetId,
                categoryId,
                categoryName,
                year,
                month,
                budgetAmount,
                spentAmount,
                remainingAmount,
                usageRate,
                alertLevel,
                triggeredThreshold,
                message,
                notificationSent,
                alertTime
            );
        }
    }
}
