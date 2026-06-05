package com.campus.trade.service;

import com.campus.trade.common.PageMeta;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.dto.review.AdminReviewModerationRequest;
import com.campus.trade.dto.review.CreateProductReviewRequest;
import com.campus.trade.dto.review.ProductRatingSummary;
import com.campus.trade.dto.review.ProductReviewResponse;
import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.Order;
import com.campus.trade.model.entity.ProductReview;
import com.campus.trade.model.enums.OrderStatus;
import com.campus.trade.model.enums.ReviewStatus;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductReviewRepository;
import com.campus.trade.repository.projection.ProductRatingAggregate;
import com.campus.trade.util.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public ProductReviewService(ProductReviewRepository reviewRepository,
                                OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ProductReviewResponse createReview(String username,
                                              Long orderId,
                                              CreateProductReviewRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getBuyer().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "无权评价该订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单未完成，无法评价");
        }
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "该订单已评价");
        }
        ProductReview review = new ProductReview();
        review.setOrder(order);
        review.setProduct(order.getProduct());
        review.setReviewer(order.getBuyer());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setImages(request.getImages());
        review.setAnonymous(request.isAnonymous());
        reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ProductReviewResponse> listProductReviews(Long productId,
                                                                       int page,
                                                                       int size,
                                                                       ReviewStatus status) {
        ReviewStatus effectiveStatus = status == null ? ReviewStatus.PUBLISHED : status;
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<ProductReview> reviewPage = reviewRepository.findByProductIdAndStatus(productId, effectiveStatus, pageable);
        List<ProductReviewResponse> responses = reviewPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PaginatedResponse<>(responses, new PageMeta(page, size, reviewPage.getTotalElements()));
    }

    @Transactional
    public ProductReviewResponse moderateReview(Long reviewId, AdminReviewModerationRequest request) {
        ProductReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "评价不存在"));
        review.setStatus(request.status());
        if (StringUtils.hasText(request.adminNote())) {
            review.setModerationNote(request.adminNote());
        }
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public ProductRatingSummary getRatingSummary(Long productId) {
        double avg = reviewRepository.findAverageRating(productId, ReviewStatus.PUBLISHED);
        long count = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.PUBLISHED);
        return new ProductRatingSummary(avg, count);
    }

    public void attachRatingSummary(Collection<ProductResponse> responses) {
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }
        List<Long> ids = responses.stream()
                .map(ProductResponse::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        List<ProductRatingAggregate> aggregates = reviewRepository.aggregateRatings(ids, ReviewStatus.PUBLISHED);
        Map<Long, ProductRatingSummary> summaryMap = new HashMap<>();
        for (ProductRatingAggregate aggregate : aggregates) {
            summaryMap.put(aggregate.getProductId(),
                    new ProductRatingSummary(
                            aggregate.getAverageRating() == null ? 0D : aggregate.getAverageRating(),
                            aggregate.getRatingCount() == null ? 0L : aggregate.getRatingCount()));
        }
        responses.forEach(resp -> {
            ProductRatingSummary summary = summaryMap.getOrDefault(resp.getId(), new ProductRatingSummary(0D, 0));
            resp.setAverageRating(summary.getAverageRating());
            resp.setRatingCount(summary.getRatingCount());
        });
    }

    public void attachRatingSummary(ProductResponse response) {
        if (response == null) {
            return;
        }
        attachRatingSummary(Collections.singletonList(response));
    }

    @Transactional(readOnly = true)
    public long countPublishedInRange(LocalDateTime start, LocalDateTime end) {
        return reviewRepository.countByStatusAndCreateTimeBetween(ReviewStatus.PUBLISHED, start, end);
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        ProductReviewResponse response = new ProductReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProduct().getId());
        response.setOrderId(review.getOrder().getId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setImages(review.getImages());
        response.setAnonymous(review.isAnonymous());
        response.setStatus(review.getStatus());
        response.setModerationNote(review.getModerationNote());
        response.setCreateTime(review.getCreateTime());
        response.setReviewer(review.isAnonymous() ? null : toSummary(review));
        return response;
    }

    private UserSummary toSummary(ProductReview review) {
        return UserMapper.toMaskedSummary(review.getReviewer());
    }
}
