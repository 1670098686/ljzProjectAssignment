package com.campus.trade.dto.favorite;

public class FavoriteStatusResponse {

    private Long productId;
    private boolean favorited;

    public FavoriteStatusResponse() {
    }

    public FavoriteStatusResponse(Long productId, boolean favorited) {
        this.productId = productId;
        this.favorited = favorited;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }
}
