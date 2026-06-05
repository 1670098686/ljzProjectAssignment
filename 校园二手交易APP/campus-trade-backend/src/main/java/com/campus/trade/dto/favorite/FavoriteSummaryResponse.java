package com.campus.trade.dto.favorite;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class FavoriteSummaryResponse {

    private long totalFavorites;
    private long onSaleFavorites;
    private long soldOutFavorites;
    private long removedFavorites;
    private LocalDateTime lastUpdatedAt;
    private List<FavoriteCategoryStat> categoryDistribution = Collections.emptyList();
    private List<FavoriteTopProductResponse> trendingProducts = Collections.emptyList();

    public long getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(long totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public long getOnSaleFavorites() {
        return onSaleFavorites;
    }

    public void setOnSaleFavorites(long onSaleFavorites) {
        this.onSaleFavorites = onSaleFavorites;
    }

    public long getSoldOutFavorites() {
        return soldOutFavorites;
    }

    public void setSoldOutFavorites(long soldOutFavorites) {
        this.soldOutFavorites = soldOutFavorites;
    }

    public long getRemovedFavorites() {
        return removedFavorites;
    }

    public void setRemovedFavorites(long removedFavorites) {
        this.removedFavorites = removedFavorites;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public List<FavoriteCategoryStat> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<FavoriteCategoryStat> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<FavoriteTopProductResponse> getTrendingProducts() {
        return trendingProducts;
    }

    public void setTrendingProducts(List<FavoriteTopProductResponse> trendingProducts) {
        this.trendingProducts = trendingProducts;
    }
}
