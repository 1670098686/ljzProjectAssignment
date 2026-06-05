package com.campus.trade.repository.projection;

import com.campus.trade.model.enums.ProductStatus;

public interface FavoriteStatusCountView {

    ProductStatus getStatus();

    long getTotal();
}
