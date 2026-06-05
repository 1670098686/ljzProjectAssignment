package com.campus.trade.dto.review;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ProductReviewResponse {

    private Long id;
    private Long productId;
    private Long orderId;
    private int rating;
    private String content;
    private List<String> images;
    private boolean anonymous;
    private ReviewStatus status;
    private String moderationNote;
    private LocalDateTime createTime;
    private UserSummary reviewer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public UserSummary getReviewer() {
        return reviewer;
    }

    public void setReviewer(UserSummary reviewer) {
        this.reviewer = reviewer;
    }
}
