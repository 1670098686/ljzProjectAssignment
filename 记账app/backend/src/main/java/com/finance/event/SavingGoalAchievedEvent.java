package com.finance.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SavingGoalAchievedEvent implements DomainEvent {

    private final Long userId;
    private final Long goalId;
    private final BigDecimal targetAmount;
    private final LocalDateTime achievedAt;

    public SavingGoalAchievedEvent(Long userId,
                                   Long goalId,
                                   BigDecimal targetAmount,
                                   LocalDateTime achievedAt) {
        this.userId = userId;
        this.goalId = goalId;
        this.targetAmount = targetAmount;
        this.achievedAt = achievedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }
}
