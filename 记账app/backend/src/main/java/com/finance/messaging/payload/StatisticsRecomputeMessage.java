package com.finance.messaging.payload;

import com.finance.event.TransactionCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StatisticsRecomputeMessage {

    private final Long userId;
    private final Long transactionId;
    private final Integer type;
    private final Long categoryId;
    private final BigDecimal amount;
    private final LocalDate transactionDate;

    private StatisticsRecomputeMessage(Long userId,
                                       Long transactionId,
                                       Integer type,
                                       Long categoryId,
                                       BigDecimal amount,
                                       LocalDate transactionDate) {
        this.userId = userId;
        this.transactionId = transactionId;
        this.type = type;
        this.categoryId = categoryId;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public static StatisticsRecomputeMessage from(TransactionCreatedEvent event) {
        return new StatisticsRecomputeMessage(
                event.getUserId(),
                event.getTransactionId(),
                event.getType(),
                event.getCategoryId(),
                event.getAmount(),
                event.getTransactionDate()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Integer getType() {
        return type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    @Override
    public String toString() {
        return "StatisticsRecomputeMessage{" +
                "userId=" + userId +
                ", transactionId=" + transactionId +
                ", type=" + type +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
