package com.finance.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionCreatedEvent implements DomainEvent {

    private final Long userId;
    private final Long transactionId;
    private final Integer type;
    private final Long categoryId;
    private final BigDecimal amount;
    private final LocalDate transactionDate;

    public TransactionCreatedEvent(Long userId,
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
}
