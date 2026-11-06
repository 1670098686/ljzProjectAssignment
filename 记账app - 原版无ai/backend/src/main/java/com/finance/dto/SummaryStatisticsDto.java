package com.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "月度统计汇总数据")
public class SummaryStatisticsDto {

    @Schema(description = "总收入", example = "18650.75")
    private BigDecimal totalIncome;

    @Schema(description = "总支出", example = "15320.10")
    private BigDecimal totalExpense;

    @Schema(description = "结余（收入-支出）", example = "3330.65")
    private BigDecimal balance;

    public SummaryStatisticsDto() {
    }

    public SummaryStatisticsDto(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = balance;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
