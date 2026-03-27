package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BudgetAlertHistoryResponse {
    
    private Long alertId;
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private Integer year;
    private Integer month;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal usageRate;
    private String alertLevel;
    private BigDecimal triggeredThreshold;
    private String message;
    private Boolean notificationSent;
    private LocalDateTime alertTime;
    private LocalDateTime resolvedTime; // 预警解除时间

    // Constructor
    public BudgetAlertHistoryResponse() {
        // Default constructor
    }

    public BudgetAlertHistoryResponse(Long alertId, Long budgetId, Long categoryId, String categoryName, Integer year, 
                                     Integer month, BigDecimal budgetAmount, BigDecimal spentAmount, BigDecimal usageRate, 
                                     String alertLevel, BigDecimal triggeredThreshold, String message, Boolean notificationSent, 
                                     LocalDateTime alertTime, LocalDateTime resolvedTime) {
        this.alertId = alertId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.year = year;
        this.month = month;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.usageRate = usageRate;
        this.alertLevel = alertLevel;
        this.triggeredThreshold = triggeredThreshold;
        this.message = message;
        this.notificationSent = notificationSent;
        this.alertTime = alertTime;
        this.resolvedTime = resolvedTime;
    }

    // Getter methods
    public Long getAlertId() {
        return alertId;
    }

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

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    // Setter methods
    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

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

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long alertId;
        private Long budgetId;
        private Long categoryId;
        private String categoryName;
        private Integer year;
        private Integer month;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal usageRate;
        private String alertLevel;
        private BigDecimal triggeredThreshold;
        private String message;
        private Boolean notificationSent;
        private LocalDateTime alertTime;
        private LocalDateTime resolvedTime;

        public Builder alertId(Long alertId) {
            this.alertId = alertId;
            return this;
        }

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

        public Builder notificationSent(Boolean notificationSent) {
            this.notificationSent = notificationSent;
            return this;
        }

        public Builder alertTime(LocalDateTime alertTime) {
            this.alertTime = alertTime;
            return this;
        }

        public Builder resolvedTime(LocalDateTime resolvedTime) {
            this.resolvedTime = resolvedTime;
            return this;
        }

        public BudgetAlertHistoryResponse build() {
            return new BudgetAlertHistoryResponse(
                alertId,
                budgetId,
                categoryId,
                categoryName,
                year,
                month,
                budgetAmount,
                spentAmount,
                usageRate,
                alertLevel,
                triggeredThreshold,
                message,
                notificationSent,
                alertTime,
                resolvedTime
            );
        }
    }

    // 分页信息
    public static class PageInfo {
        private Integer currentPage;
        private Integer pageSize;
        private Long totalRecords;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        // Constructor
        public PageInfo() {
            // Default constructor
        }

        public PageInfo(Integer currentPage, Integer pageSize, Long totalRecords, Integer totalPages, Boolean hasNext, Boolean hasPrevious) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalRecords = totalRecords;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        // Getter methods
        public Integer getCurrentPage() {
            return currentPage;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public Long getTotalRecords() {
            return totalRecords;
        }

        public Integer getTotalPages() {
            return totalPages;
        }

        public Boolean getHasNext() {
            return hasNext;
        }

        public Boolean getHasPrevious() {
            return hasPrevious;
        }

        // Setter methods
        public void setCurrentPage(Integer currentPage) {
            this.currentPage = currentPage;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public void setTotalRecords(Long totalRecords) {
            this.totalRecords = totalRecords;
        }

        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }

        public void setHasNext(Boolean hasNext) {
            this.hasNext = hasNext;
        }

        public void setHasPrevious(Boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Integer currentPage;
            private Integer pageSize;
            private Long totalRecords;
            private Integer totalPages;
            private Boolean hasNext;
            private Boolean hasPrevious;

            public Builder currentPage(Integer currentPage) {
                this.currentPage = currentPage;
                return this;
            }

            public Builder pageSize(Integer pageSize) {
                this.pageSize = pageSize;
                return this;
            }

            public Builder totalRecords(Long totalRecords) {
                this.totalRecords = totalRecords;
                return this;
            }

            public Builder totalPages(Integer totalPages) {
                this.totalPages = totalPages;
                return this;
            }

            public Builder hasNext(Boolean hasNext) {
                this.hasNext = hasNext;
                return this;
            }

            public Builder hasPrevious(Boolean hasPrevious) {
                this.hasPrevious = hasPrevious;
                return this;
            }

            public PageInfo build() {
                return new PageInfo(
                    currentPage,
                    pageSize,
                    totalRecords,
                    totalPages,
                    hasNext,
                    hasPrevious
                );
            }
        }
    }

    // 预算预警历史列表响应
    public static class BudgetAlertHistoryListResponse {
        private List<BudgetAlertHistoryResponse> records;
        private PageInfo pageInfo;

        // Constructor
        public BudgetAlertHistoryListResponse() {
            // Default constructor
        }

        public BudgetAlertHistoryListResponse(List<BudgetAlertHistoryResponse> records, PageInfo pageInfo) {
            this.records = records;
            this.pageInfo = pageInfo;
        }

        // Getter methods
        public List<BudgetAlertHistoryResponse> getRecords() {
            return records;
        }

        public PageInfo getPageInfo() {
            return pageInfo;
        }

        // Setter methods
        public void setRecords(List<BudgetAlertHistoryResponse> records) {
            this.records = records;
        }

        public void setPageInfo(PageInfo pageInfo) {
            this.pageInfo = pageInfo;
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<BudgetAlertHistoryResponse> records;
            private PageInfo pageInfo;

            public Builder records(List<BudgetAlertHistoryResponse> records) {
                this.records = records;
                return this;
            }

            public Builder pageInfo(PageInfo pageInfo) {
                this.pageInfo = pageInfo;
                return this;
            }

            public BudgetAlertHistoryListResponse build() {
                return new BudgetAlertHistoryListResponse(
                    records,
                    pageInfo
                );
            }
        }
    }
}