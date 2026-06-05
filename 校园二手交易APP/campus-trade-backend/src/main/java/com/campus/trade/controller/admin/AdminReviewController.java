package com.campus.trade.controller.admin;

import com.campus.trade.annotation.OperationLog;
import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.review.AdminReviewModerationRequest;
import com.campus.trade.dto.review.ProductReviewResponse;
import com.campus.trade.model.enums.OperationType;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize(AccessExpressions.ADMIN)
public class AdminReviewController {

    private final ProductReviewService reviewService;

    public AdminReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{reviewId}/moderation")
        @OperationLog(title = "评价审核", action = "MODERATE_REVIEW", type = OperationType.REVIEW,
            resourceId = "#{#reviewId}")
    public ApiResponse<ProductReviewResponse> moderateReview(@PathVariable Long reviewId,
                                                              @Valid @RequestBody AdminReviewModerationRequest request) {
        return ApiResponse.success(reviewService.moderateReview(reviewId, request));
    }
}
