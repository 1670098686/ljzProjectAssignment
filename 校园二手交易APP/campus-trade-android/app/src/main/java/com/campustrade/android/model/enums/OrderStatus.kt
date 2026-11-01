package com.campustrade.android.model.enums

enum class OrderStatus(val displayName: String) {
    PENDING("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");
    
    companion object {
        fun fromString(status: String): OrderStatus {
            return values().find { it.name == status } ?: PENDING
        }
    }
}