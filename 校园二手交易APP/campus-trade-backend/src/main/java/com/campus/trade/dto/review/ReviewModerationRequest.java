package com.campus.trade.dto.review;

import com.campus.trade.model.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public class ReviewModerationRequest {

    @NotNull
    private ReviewStatus status;

    private String note;

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
