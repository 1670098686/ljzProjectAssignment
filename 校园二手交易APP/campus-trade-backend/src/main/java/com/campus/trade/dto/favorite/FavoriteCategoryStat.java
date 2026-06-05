package com.campus.trade.dto.favorite;

import com.campus.trade.model.enums.ProductCategory;

public class FavoriteCategoryStat {

    private ProductCategory category;
    private long count;
    private double ratio;

    public FavoriteCategoryStat() {
    }

    public FavoriteCategoryStat(ProductCategory category, long count, double ratio) {
        this.category = category;
        this.count = count;
        this.ratio = ratio;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}
