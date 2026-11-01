package com.campustrade.android.repository

import com.campustrade.android.model.Product
import com.campustrade.android.model.enums.AuditStatus
import com.campustrade.android.model.enums.ProductCategory
import com.campustrade.android.model.enums.ProductStatus
import com.campustrade.android.ui.profile.model.OrderSummary
import com.campustrade.android.ui.profile.model.ProfileData
import com.campustrade.android.ui.profile.model.Statistics
import com.campustrade.android.ui.profile.model.UserInfo
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

open class ProfileRepository {

    /**
     * 获取个人中心数据
     */
    open suspend fun getProfileData(userId: Long): ProfileData {
        // 模拟数据，实际应该从API获取
        return ProfileData(
            userInfo = getUserInfo(userId),
            statistics = getStatistics(userId),
            favoriteProducts = getFavoriteProducts(userId),
            publishedProducts = getPublishedProducts(userId),
            orderHistory = getOrderHistory(userId)
        )
    }

    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(userId: Long): UserInfo {
        // 模拟数据
        return UserInfo(
            id = userId,
            username = "student_${userId}",
            email = "student${userId}@example.com",
            phone = "13800138000",
            realName = "张三",
            school = "北京大学",
            avatar = null,
            contactInfo = "微信：student_${userId}",
            role = "STUDENT",
            status = "ACTIVE",
            emailVerified = true,
            lastLogin = "2024-01-15 10:30:00"
        )
    }

    /**
     * 获取统计信息
     */
    suspend fun getStatistics(userId: Long): Statistics {
        // 模拟数据
        return Statistics(
            publishedCount = 5,
            soldCount = 3,
            purchasedCount = 2,
            favoriteCount = 8,
            totalRevenue = 450.0,
            totalSpent = 280.0
        )
    }

    /**
     * 获取收藏的商品
     */
    suspend fun getFavoriteProducts(userId: Long): List<Product> {
        // 模拟数据
        return listOf(
            Product(
                id = 1,
                title = "二手iPhone 13",
                description = "九成新，使用一年，无划痕",
                price = BigDecimal("3500.00"),
                category = ProductCategory.ELECTRONICS,
                images = listOf("https://example.com/iphone13.jpg"),
                status = ProductStatus.ON_SALE,
                auditStatus = AuditStatus.APPROVED,
                viewCount = 120,
                likeCount = 15,
                contactInfo = "微信：seller123",
                location = "北京大学",
                remark = "支持面交"
            ),
            Product(
                id = 2,
                title = "Java编程思想",
                description = "经典编程书籍，几乎全新",
                price = BigDecimal("45.00"),
                category = ProductCategory.BOOKS,
                images = listOf("https://example.com/java_book.jpg"),
                status = ProductStatus.ON_SALE,
                auditStatus = AuditStatus.APPROVED,
                viewCount = 80,
                likeCount = 8,
                contactInfo = "电话：13800138000",
                location = "清华大学",
                remark = "仅限校内交易"
            )
        )
    }

    /**
     * 获取发布的商品
     */
    suspend fun getPublishedProducts(userId: Long): List<Product> {
        // 模拟数据
        return listOf(
            Product(
                id = 3,
                title = "二手笔记本电脑",
                description = "联想小新，配置良好",
                price = BigDecimal("2500.00"),
                category = ProductCategory.ELECTRONICS,
                images = listOf("https://example.com/laptop.jpg"),
                status = ProductStatus.ON_SALE,
                auditStatus = AuditStatus.APPROVED,
                viewCount = 150,
                likeCount = 12,
                contactInfo = "微信：seller_${'$'}{userId}",
                location = "北京大学",
                remark = "可小刀"
            ),
            Product(
                id = 4,
                title = "考研数学资料",
                description = "全套考研数学复习资料",
                price = BigDecimal("80.00"),
                category = ProductCategory.BOOKS,
                images = listOf("https://example.com/math_material.jpg"),
                status = ProductStatus.SOLD,
                auditStatus = AuditStatus.APPROVED,
                viewCount = 60,
                likeCount = 5,
                contactInfo = "电话：13800138000",
                location = "北京大学",
                remark = "已售出"
            )
        )
    }

    /**
     * 获取订单历史
     */
    suspend fun getOrderHistory(userId: Long): List<OrderSummary> {
        // 模拟数据
        return listOf(
            OrderSummary(
                id = 1001,
                productTitle = "高等数学教材",
                productImage = "https://example.com/calculus.jpg",
                price = 98.0,
                status = "COMPLETED",
                createTime = "2024-01-10 14:20",
                sellerName = "seller_${'$'}{userId}",
                buyerName = "student_${userId}"
            ),
            OrderSummary(
                id = 1002,
                productTitle = "英语六级资料",
                productImage = "https://example.com/cet6.jpg",
                price = 60.0,
                status = "SHIPPED",
                createTime = "2024-01-05 09:15",
                sellerName = "seller_${'$'}{userId}",
                buyerName = "student_${userId}"
            )
        )
    }
    suspend fun uploadAvatar(userId: Long, avatarFile: java.io.File): String? {
        // 模拟上传操作
        return "https://example.com/avatars/user_${userId}.jpg"
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUserInfo(userInfo: UserInfo): Boolean {
        // 模拟更新逻辑，实际项目应调用后端接口
        return true
    }

    /**
     * 退出登录
     */
    suspend fun logout(): Boolean {
        // 模拟退出操作
        return true
    }
}