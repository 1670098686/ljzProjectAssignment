package com.campus.trade.dto.review;

import com.campus.trade.model.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReviewModerationRequest(
        @NotNull ReviewStatus status,
        String adminNote
) {
}
