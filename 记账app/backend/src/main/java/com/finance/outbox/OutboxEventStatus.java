package com.finance.outbox;

public enum OutboxEventStatus {
    PENDING,
    RETRY,
    COMPLETED,
    FAILED
}
