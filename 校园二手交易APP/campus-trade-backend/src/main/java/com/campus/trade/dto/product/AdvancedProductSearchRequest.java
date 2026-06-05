package com.campus.trade.dto.product;

import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductSearchSort;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.util.SearchKeywordParser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdvancedProductSearchRequest {

    private List<ProductCategory> categories;
    private List<Long> categoryIds;
    private List<ProductStatus> statuses;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;
    private List<String> keywords;
    private String location;
    private List<String> locationKeywords;
    private String sellerSchool;
    private Boolean includeInactive;
    private Integer publishedWithinDays;
    private Boolean onlyWithImages;
    private ProductSearchSort sortMode;
    private String sortBy;
    private Sort.Direction direction = Sort.Direction.DESC;
    private List<ProductSearchClause> clauses;

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(50)
    private int size = 10;

    public List<ProductCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ProductCategory> categories) {
        this.categories = categories;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<ProductStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<ProductStatus> statuses) {
        this.statuses = statuses;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSellerSchool() {
        return sellerSchool;
    }

    public void setSellerSchool(String sellerSchool) {
        this.sellerSchool = sellerSchool;
    }

    public List<String> getLocationKeywords() {
        return locationKeywords;
    }

    public void setLocationKeywords(List<String> locationKeywords) {
        this.locationKeywords = locationKeywords;
    }

    public Boolean getIncludeInactive() {
        return includeInactive;
    }

    public void setIncludeInactive(Boolean includeInactive) {
        this.includeInactive = includeInactive;
    }

    public Integer getPublishedWithinDays() {
        return publishedWithinDays;
    }

    public void setPublishedWithinDays(Integer publishedWithinDays) {
        this.publishedWithinDays = publishedWithinDays;
    }

    public Boolean getOnlyWithImages() {
        return onlyWithImages;
    }

    public void setOnlyWithImages(Boolean onlyWithImages) {
        this.onlyWithImages = onlyWithImages;
    }

    public ProductSearchSort getSortMode() {
        return sortMode;
    }

    public void setSortMode(ProductSearchSort sortMode) {
        this.sortMode = sortMode;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Sort.Direction getDirection() {
        return direction;
    }

    public void setDirection(Sort.Direction direction) {
        this.direction = direction;
    }

    public List<ProductSearchClause> getClauses() {
        return clauses;
    }

    public void setClauses(List<ProductSearchClause> clauses) {
        this.clauses = clauses;
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

    public ProductSearchFilter toFilter() {
        normalize();
        boolean allowInactive = Boolean.TRUE.equals(includeInactive) || shouldIncludeInactive();
        List<String> effectiveKeywords = keywords;
        if (CollectionUtils.isEmpty(effectiveKeywords) && StringUtils.hasText(keyword)) {
            effectiveKeywords = SearchKeywordParser.parse(keyword);
        }
        ProductSearchFilter.Builder builder = ProductSearchFilter.builder()
                .keyword(keyword)
                .category(CollectionUtils.isEmpty(categories) ? null : categories.get(0))
                .categories(categories)
            .categoryId(CollectionUtils.isEmpty(categoryIds) ? null : categoryIds.get(0))
            .categoryIds(categoryIds)
                .statuses(statuses)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .location(location)
                .locationKeywords(locationKeywords)
                .sellerSchool(sellerSchool)
                .includeInactive(allowInactive)
                .onlyWithImages(onlyWithImages)
                .publishedWithinDays(publishedWithinDays)
                .keywordTokens(effectiveKeywords)
                .clauses(clauses);
        return builder.build();
    }

    public void normalize() {
        if (!CollectionUtils.isEmpty(categories)) {
            categories = categories.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(categoryIds)) {
            categoryIds = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        }
        if (!CollectionUtils.isEmpty(statuses)) {
            statuses = statuses.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }
        if ((keywords == null || keywords.isEmpty()) && StringUtils.hasText(keyword)) {
            keywords = SearchKeywordParser.parse(keyword);
        }
        if ((locationKeywords == null || locationKeywords.isEmpty()) && StringUtils.hasText(location)) {
            locationKeywords = SearchKeywordParser.parse(location);
        }
        if (!CollectionUtils.isEmpty(locationKeywords)) {
            locationKeywords = locationKeywords.stream()
                    .filter(StringUtils::hasText)
                    .map(token -> token.length() > 32 ? token.substring(0, 32) : token)
                    .map(String::trim)
                    .map(token -> token.toLowerCase(Locale.ENGLISH))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
        }
        if (publishedWithinDays != null && publishedWithinDays <= 0) {
            publishedWithinDays = null;
        }
        if (onlyWithImages == null) {
            onlyWithImages = Boolean.FALSE;
        }
        if (includeInactive == null) {
            includeInactive = shouldIncludeInactive();
        }
        if (clauses != null && !clauses.isEmpty()) {
            clauses = clauses.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 50));
    }

    public boolean shouldIncludeInactive() {
        if (Boolean.TRUE.equals(includeInactive)) {
            return true;
        }
        if (CollectionUtils.isEmpty(statuses)) {
            return false;
        }
        return statuses.stream().anyMatch(status -> status != ProductStatus.ON_SALE);
    }
}
