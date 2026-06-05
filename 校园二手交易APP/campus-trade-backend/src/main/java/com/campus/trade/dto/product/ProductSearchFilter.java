package com.campus.trade.dto.product;

import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ProductSearchFilter {

    private ProductCategory category;
    private List<ProductCategory> categories;
    private Long categoryId;
    private List<Long> categoryIds;
    private ProductStatus status;
    private List<ProductStatus> statuses;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;
    private List<String> keywordTokens;
    private String location;
    private List<String> locationKeywords;
    private String sellerSchool;
    private boolean includeInactive;
    private Boolean onlyWithImages;
    private Integer publishedWithinDays;
    private LocalDateTime publishedAfter;
    private List<ProductSearchClause> clauses;

    private ProductSearchFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProductCategory getCategory() {
        return category;
    }

    public List<ProductCategory> getCategories() {
        return categories;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public List<ProductStatus> getStatuses() {
        return statuses;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public String getKeyword() {
        return keyword;
    }

    public List<String> getKeywordTokens() {
        return keywordTokens;
    }

    public String getLocation() {
        return location;
    }

    public String getSellerSchool() {
        return sellerSchool;
    }

    public List<String> getLocationKeywords() {
        return locationKeywords;
    }

    public boolean isIncludeInactive() {
        return includeInactive;
    }

    public Boolean getOnlyWithImages() {
        return onlyWithImages;
    }

    public Integer getPublishedWithinDays() {
        return publishedWithinDays;
    }

    public LocalDateTime getPublishedAfter() {
        return publishedAfter;
    }

    public List<ProductSearchClause> getClauses() {
        return clauses;
    }

    public static final class Builder {
        private final ProductSearchFilter filter = new ProductSearchFilter();

        private Builder() {
        }

        public Builder category(ProductCategory category) {
            filter.category = category;
            return this;
        }

        public Builder categories(List<ProductCategory> categories) {
            filter.categories = categories == null || categories.isEmpty()
                    ? null : List.copyOf(categories);
            return this;
        }

        public Builder categoryId(Long categoryId) {
            filter.categoryId = categoryId;
            return this;
        }

        public Builder categoryIds(List<Long> categoryIds) {
            filter.categoryIds = categoryIds == null || categoryIds.isEmpty()
                    ? null : List.copyOf(categoryIds);
            return this;
        }

        public Builder status(ProductStatus status) {
            filter.status = status;
            return this;
        }

        public Builder statuses(List<ProductStatus> statuses) {
            filter.statuses = statuses == null || statuses.isEmpty()
                    ? null : List.copyOf(statuses);
            return this;
        }

        public Builder minPrice(BigDecimal minPrice) {
            filter.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(BigDecimal maxPrice) {
            filter.maxPrice = maxPrice;
            return this;
        }

        public Builder keyword(String keyword) {
            filter.keyword = keyword;
            return this;
        }

        public Builder keywordTokens(List<String> keywords) {
            filter.keywordTokens = keywords == null || keywords.isEmpty()
                    ? Collections.emptyList()
                    : List.copyOf(keywords);
            return this;
        }

        public Builder location(String location) {
            filter.location = location;
            return this;
        }

        public Builder locationKeywords(List<String> locationKeywords) {
            filter.locationKeywords = (locationKeywords == null || locationKeywords.isEmpty())
                    ? Collections.emptyList()
                    : List.copyOf(locationKeywords);
            return this;
        }

        public Builder sellerSchool(String sellerSchool) {
            filter.sellerSchool = sellerSchool;
            return this;
        }

        public Builder includeInactive(boolean includeInactive) {
            filter.includeInactive = includeInactive;
            return this;
        }

        public Builder onlyWithImages(Boolean onlyWithImages) {
            filter.onlyWithImages = onlyWithImages;
            return this;
        }

        public Builder publishedWithinDays(Integer publishedWithinDays) {
            filter.publishedWithinDays = publishedWithinDays;
            return this;
        }

        public Builder publishedAfter(LocalDateTime publishedAfter) {
            filter.publishedAfter = publishedAfter;
            return this;
        }

        public Builder clauses(List<ProductSearchClause> clauses) {
            filter.clauses = clauses == null || clauses.isEmpty() ? null : List.copyOf(clauses);
            return this;
        }

        public ProductSearchFilter build() {
            return filter;
        }
    }
}
