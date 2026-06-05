package com.campus.trade.util;

import com.campus.trade.model.enums.ProductSearchSort;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public final class ProductSortResolver {

    private ProductSortResolver() {
    }

    public static Sort resolve(ProductSearchSort sortMode,
                               String keyword,
                               Sort.Direction direction,
                               String sortBy) {
        ProductSearchSort effective = determineSortMode(sortMode, keyword);
        if (effective != null) {
            return effective.toSort();
        }
        Sort.Direction safeDirection = direction == null ? Sort.Direction.DESC : direction;
        String property = StringUtils.hasText(sortBy) ? sortBy : "createTime";
        return Sort.by(safeDirection, property);
    }

    public static Sort recommendedSort() {
        return ProductSearchSort.RECOMMENDED.toSort();
    }

    private static ProductSearchSort determineSortMode(ProductSearchSort sortMode, String keyword) {
        if (sortMode != null) {
            return sortMode;
        }
        if (StringUtils.hasText(keyword)) {
            return ProductSearchSort.RELEVANCE;
        }
        return null;
    }
}
