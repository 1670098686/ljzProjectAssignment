package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TrendPointDto {
    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;

    public TrendPointDto() {
    }

    public TrendPointDto(LocalDate date, BigDecimal income, BigDecimal expense) {
        this.date = date;
        this.income = income;
        this.expense = expense;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }
}
