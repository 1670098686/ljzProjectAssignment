package com.finance.messaging.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finance.dto.BudgetAlertDto;
import com.finance.event.BudgetAlertEvent;
import com.finance.event.BudgetOverspendEvent;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BudgetEventMessage {

    public enum EventType {
        ALERT,
        OVERSPEND
    }

    private final EventType eventType;
    private final Long userId;
    private final Long budgetId;
    private final Long categoryId;
    private final BigDecimal budgetAmount;
    private final BigDecimal spentAmount;
    private final Integer year;
    private final Integer month;
    private final BudgetAlertDto alert;

    private BudgetEventMessage(EventType eventType,
                               Long userId,
                               Long budgetId,
                               Long categoryId,
                               BigDecimal budgetAmount,
                               BigDecimal spentAmount,
                               Integer year,
                               Integer month,
                               BudgetAlertDto alert) {
        this.eventType = eventType;
        this.userId = userId;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.year = year;
        this.month = month;
        this.alert = alert;
    }

    public static BudgetEventMessage from(BudgetAlertEvent event) {
        return new BudgetEventMessage(
                EventType.ALERT,
                event.getUserId(),
                null,
                event.getAlert().getCategoryId(),
                null,
                null,
                event.getAlert().getYear(),
                event.getAlert().getMonth(),
                event.getAlert()
        );
    }

    public static BudgetEventMessage from(BudgetOverspendEvent event) {
        return new BudgetEventMessage(
                EventType.OVERSPEND,
                event.getUserId(),
                event.getBudgetId(),
                event.getCategoryId(),
                event.getBudgetAmount(),
                event.getSpentAmount(),
                event.getYear(),
                event.getMonth(),
                null
        );
    }

    public EventType getEventType() {
        return eventType;
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

    public BudgetAlertDto getAlert() {
        return alert;
    }

    @Override
    public String toString() {
        return "BudgetEventMessage{" +
                "eventType=" + eventType +
                ", userId=" + userId +
                ", budgetId=" + budgetId +
                ", categoryId=" + categoryId +
                ", budgetAmount=" + budgetAmount +
                ", spentAmount=" + spentAmount +
                ", year=" + year +
                ", month=" + month +
                '}';
    }
}
