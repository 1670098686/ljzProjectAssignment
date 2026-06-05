package com.campus.trade.dto.review;

public class ProductRatingSummary {

    private double averageRating;
    private long ratingCount;

    public ProductRatingSummary() {
    }

    public ProductRatingSummary(double averageRating, long ratingCount) {
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }
}
