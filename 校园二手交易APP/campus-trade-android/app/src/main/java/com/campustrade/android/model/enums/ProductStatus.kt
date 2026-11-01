package com.campustrade.android.model.enums

/**
 * 商品状态枚举。
 */
enum class ProductStatus(val displayName: String) {
    ON_SALE("在售"),
    OFF_SALE("已下架"),
    SOLD("已售出"),
    DELETED("已删除")
}
