package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.review.CreateProductReviewRequest;
import com.campus.trade.dto.review.ProductReviewResponse;
import com.campus.trade.dto.review.ProductRatingSummary;
import com.campus.trade.model.enums.ReviewStatus;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProductReviewController {

    private final ProductReviewService reviewService;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/orders/{orderId}/reviews")
    @PreAuthorize(AccessExpressions.MEMBER)
    public ApiResponse<ProductReviewResponse> createReview(@PathVariable Long orderId,
                                                           @Valid @RequestBody CreateProductReviewRequest request) {
        ProductReviewResponse response = reviewService.createReview(SecurityUtils.getCurrentUsername(), orderId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<PaginatedResponse<ProductReviewResponse>> listReviews(@PathVariable Long productId,
                                                                             @RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size,
                                                                             @RequestParam(required = false) ReviewStatus status) {
        return ApiResponse.success(reviewService.listProductReviews(productId, page, size, status));
    }

    @GetMapping("/products/{productId}/rating")
    public ApiResponse<ProductRatingSummary> getRating(@PathVariable Long productId) {
        return ApiResponse.success(reviewService.getRatingSummary(productId));
    }
}
