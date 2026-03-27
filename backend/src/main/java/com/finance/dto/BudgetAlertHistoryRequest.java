package com.finance.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BudgetAlertHistoryRequest {
    
    private Integer year; // 可选，指定年份
    
    private Integer month; // 可选，指定月份
    
    private Integer quarter; // 可选，指定季度（1-4）
    
    private LocalDateTime startDate; // 可选，开始日期
    
    private LocalDateTime endDate; // 可选，结束日期
    
    private List<String> alertLevels; // 可选，预警级别列表（WARNING, CRITICAL）
    
    private Long categoryId; // 可选，指定分类ID
    
    private Integer page = 1; // 页码，默认1
    
    private Integer size = 20; // 每页大小，默认20

    // Getter methods
    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public List<String> getAlertLevels() {
        return alertLevels;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    // Setter methods
    public void setYear(Integer year) {
        this.year = year;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setAlertLevels(List<String> alertLevels) {
        this.alertLevels = alertLevels;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}