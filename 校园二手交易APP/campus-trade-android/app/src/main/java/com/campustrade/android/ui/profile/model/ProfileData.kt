package com.campustrade.android.ui.profile.model

import com.campustrade.android.model.Product

/**
 * 个人中心数据类
 */
data class ProfileData(
    val userInfo: UserInfo,
    val statistics: Statistics,
    val favoriteProducts: List<Product>,
    val publishedProducts: List<Product>,
    val orderHistory: List<OrderSummary>
)

/**
 * 用户信息
 */
data class UserInfo(
    val id: Long,
    val username: String,
    val email: String,
    val phone: String?,
    val realName: String?,
    val school: String?,
    val avatar: String?,
    val contactInfo: String?,
    val role: String,
    val status: String,
    val emailVerified: Boolean,
    val lastLogin: String?
)

/**
 * 统计信息
 */
data class Statistics(
    val publishedCount: Int = 0,
    val soldCount: Int = 0,
    val purchasedCount: Int = 0,
    val favoriteCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalSpent: Double = 0.0
)

/**
 * 订单摘要
 */
data class OrderSummary(
    val id: Long,
    val productTitle: String,
    val productImage: String?,
    val price: Double,
    val status: String,
    val createTime: String,
    val sellerName: String,
    val buyerName: String
)

/**
 * 个人中心菜单项
 */
data class ProfileMenuItem(
    val id: Int,
    val title: String,
    val icon: Int,
    val count: Int = 0,
    val showBadge: Boolean = false
)