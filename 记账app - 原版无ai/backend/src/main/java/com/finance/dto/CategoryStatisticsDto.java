package com.finance.dto;

import java.math.BigDecimal;

public class CategoryStatisticsDto {
    private String categoryName;
    private BigDecimal amount;

    public CategoryStatisticsDto() {
    }

    public CategoryStatisticsDto(String categoryName, BigDecimal amount) {
        this.categoryName = categoryName;
        this.amount = amount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
