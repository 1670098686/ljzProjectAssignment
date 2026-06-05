package com.campus.trade.dto.user;

import com.campus.trade.model.enums.AccountStatus;

import java.time.LocalDateTime;

public class AccountStatusResponse {

    private boolean deleteRequested;
    private String deleteReason;
    private LocalDateTime deleteScheduleTime;
    private AccountStatus status;

    public boolean isDeleteRequested() {
        return deleteRequested;
    }

    public void setDeleteRequested(boolean deleteRequested) {
        this.deleteRequested = deleteRequested;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public LocalDateTime getDeleteScheduleTime() {
        return deleteScheduleTime;
    }

    public void setDeleteScheduleTime(LocalDateTime deleteScheduleTime) {
        this.deleteScheduleTime = deleteScheduleTime;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
