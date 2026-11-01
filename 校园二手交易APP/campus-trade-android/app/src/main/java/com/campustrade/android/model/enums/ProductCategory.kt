package com.campustrade.android.model.enums

/**
 * 商品分类枚举，与后端保持一致，同时提供中文名用于展示。
 */
enum class ProductCategory(val displayName: String) {
    BOOKS("图书教材"),
    ELECTRONICS("数码设备"),
    CLOTHING("服饰鞋包"),
    SPORTS("运动户外"),
    DAILY("日用百货"),
    OTHER("其他")
}
