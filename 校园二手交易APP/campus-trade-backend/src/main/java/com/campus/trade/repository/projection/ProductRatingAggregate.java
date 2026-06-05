package com.campus.trade.repository.projection;

public interface ProductRatingAggregate {

    Long getProductId();

    Double getAverageRating();

    Long getRatingCount();
}
