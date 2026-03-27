package com.finance.event;

import com.finance.dto.BudgetAlertDto;

public class BudgetAlertEvent implements DomainEvent {

    private final Long userId;
    private final BudgetAlertDto alert;

    public BudgetAlertEvent(Long userId, BudgetAlertDto alert) {
        this.userId = userId;
        this.alert = alert;
    }

    public Long getUserId() {
        return userId;
    }

    public BudgetAlertDto getAlert() {
        return alert;
    }
}
