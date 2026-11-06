package com.finance.event;

import java.math.BigDecimal;

public class BudgetOverspendEvent implements DomainEvent {

    private final Long userId;
    private final Long budgetId;
    private final Long categoryId;
    private final BigDecimal budgetAmount;
    private final BigDecimal spentAmount;
    private final Integer year;
    private final Integer month;

    public BudgetOverspendEvent(Long userId,
                                Long budgetId,
                                Long categoryId,
                                BigDecimal budgetAmount,
                                BigDecimal spentAmount,
                                Integer year,
                                Integer month) {
        this.userId = userId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.year = year;
        this.month = month;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }
}
