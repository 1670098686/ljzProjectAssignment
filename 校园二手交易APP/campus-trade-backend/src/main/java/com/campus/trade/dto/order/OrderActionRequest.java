package com.campus.trade.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class OrderActionRequest {

    private String reason;

    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
