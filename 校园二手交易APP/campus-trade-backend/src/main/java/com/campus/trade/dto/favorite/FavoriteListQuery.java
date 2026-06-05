package com.campus.trade.dto.favorite;

import com.campus.trade.model.enums.FavoriteSortMode;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;

public class FavoriteListQuery {

    private int page = 1;
    private int size = 10;
    private ProductCategory category;
    private ProductStatus status;
    private String keyword;
    private FavoriteSortMode sortMode = FavoriteSortMode.LATEST;
    private Boolean onlyAvailable;

    public FavoriteListQuery() {
    }

    public FavoriteListQuery(int page,
                             int size,
                             ProductCategory category,
                             ProductStatus status,
                             String keyword,
                             FavoriteSortMode sortMode,
                             Boolean onlyAvailable) {
        this.page = page;
        this.size = size;
        this.category = category;
        this.status = status;
        this.keyword = keyword;
        this.sortMode = sortMode == null ? FavoriteSortMode.LATEST : sortMode;
        this.onlyAvailable = onlyAvailable;
    }

    public static FavoriteListQuery of(int page,
                                       int size,
                                       ProductCategory category,
                                       ProductStatus status,
                                       String keyword,
                                       FavoriteSortMode sortMode,
                                       Boolean onlyAvailable) {
        return new FavoriteListQuery(page, size, category, status, keyword, sortMode, onlyAvailable);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public FavoriteSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(FavoriteSortMode sortMode) {
        this.sortMode = sortMode;
    }

    public Boolean getOnlyAvailable() {
        return onlyAvailable;
    }

    public void setOnlyAvailable(Boolean onlyAvailable) {
        this.onlyAvailable = onlyAvailable;
    }
}
