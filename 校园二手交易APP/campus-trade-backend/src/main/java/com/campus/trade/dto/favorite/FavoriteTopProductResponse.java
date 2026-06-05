package com.campus.trade.dto.favorite;

public class FavoriteTopProductResponse {

    private Long productId;
    private String productTitle;
    private long favoriteCount;

    public FavoriteTopProductResponse() {
    }

    public FavoriteTopProductResponse(Long productId, String productTitle, long favoriteCount) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.favoriteCount = favoriteCount;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }
}
