package com.campus.trade.model.enums;

import org.springframework.data.domain.Sort;

public enum ProductSearchSort {
    RELEVANCE,
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    POPULAR,
    RECOMMENDED;

    public Sort toSort() {
        return switch (this) {
            case RELEVANCE -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createTime"));
            case NEWEST -> Sort.by(Sort.Order.desc("createTime"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createTime"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createTime"));
            case POPULAR -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("viewCount"));
            case RECOMMENDED -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("viewCount"), Sort.Order.desc("createTime"));
        };
    }
}
