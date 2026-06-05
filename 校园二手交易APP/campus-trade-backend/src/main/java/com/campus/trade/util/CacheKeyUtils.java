package com.campus.trade.util;

import com.campus.trade.dto.product.ProductSearchFilter;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.StringJoiner;

public final class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    public static String productListKey(ProductCategory category,
                                        String keyword,
                                        ProductStatus status,
                                        int page,
                                        int size,
                                        Sort sort) {
        return new StringBuilder("product:list:")
                .append("category=").append(enumPart(category))
                .append("|status=").append(enumPart(status))
                .append("|keyword=").append(normalize(keyword))
                .append("|page=").append(page)
                .append("|size=").append(size)
                .append("|sort=").append(sortPart(sort))
                .toString();
    }

    public static String productSearchKey(ProductSearchFilter filter,
                                          int page,
                                          int size,
                                          Sort sort) {
        if (filter == null) {
            return new StringBuilder("product:search:null|")
                    .append("page=").append(page)
                    .append("|size=").append(size)
                    .append("|sort=").append(sortPart(sort))
                    .toString();
        }
        return new StringBuilder("product:search:")
                .append("category=").append(enumPart(filter.getCategory()))
            .append("|categoryId=").append(filter.getCategoryId() == null ? "_" : filter.getCategoryId())
                .append("|status=").append(enumPart(filter.getStatus()))
                .append("|keyword=").append(normalize(filter.getKeyword()))
                .append("|min=").append(decimalPart(filter.getMinPrice()))
                .append("|max=").append(decimalPart(filter.getMaxPrice()))
                .append("|location=").append(normalize(filter.getLocation()))
                .append("|school=").append(normalize(filter.getSellerSchool()))
                .append("|inactive=").append(filter.isIncludeInactive())
                .append("|page=").append(page)
                .append("|size=").append(size)
                .append("|sort=").append(sortPart(sort))
                .toString();
    }

    public static String searchSuggestionKey(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        return new StringBuilder("search:suggestion:")
                .append("keyword=").append(normalize(keyword))
                .append("|limit=").append(safeLimit)
                .toString();
    }

    public static String adminOrderReportKey(int days, int top) {
        int safeDays = clamp(days, 1, 30);
        int safeTop = clamp(top, 1, 10);
        return new StringBuilder("admin:order-report:")
                .append("days=").append(safeDays)
                .append("|top=").append(safeTop)
                .toString();
    }

    public static String dashboardOverviewKey(int days) {
        int safeDays = clamp(days, 1, 30);
        return "admin:dashboard:overview:days=" + safeDays;
    }

    public static String dashboardTrendKey(String metric, int days) {
        int safeDays = clamp(days, 1, 30);
        return new StringBuilder("admin:dashboard:trend:")
                .append("metric=").append(normalize(metric))
                .append("|days=").append(safeDays)
                .toString();
    }

    public static String dashboardRankingKey(String type, int limit) {
        int safeLimit = clamp(limit, 3, 20);
        return new StringBuilder("admin:dashboard:ranking:")
                .append("type=").append(normalize(type))
                .append("|limit=").append(safeLimit)
                .toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private static String enumPart(Enum<?> value) {
        return value == null ? "_" : value.name();
    }

    private static String decimalPart(BigDecimal value) {
        return value == null ? "_" : value.stripTrailingZeros().toPlainString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "_";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String sortPart(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "unsorted";
        }
        StringJoiner joiner = new StringJoiner(",");
        sort.iterator().forEachRemaining(order ->
                joiner.add(order.getProperty() + ":" + order.getDirection().name()));
        return joiner.toString();
    }
}
