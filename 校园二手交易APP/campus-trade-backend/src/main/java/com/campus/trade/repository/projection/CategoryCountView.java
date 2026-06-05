package com.campus.trade.repository.projection;

import com.campus.trade.model.enums.ProductCategory;

public interface CategoryCountView {

    ProductCategory getCategory();

    long getTotal();
}
