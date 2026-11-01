package com.campustrade.android.model

import com.campustrade.android.model.enums.AuditStatus
import com.campustrade.android.model.enums.ProductCategory
import com.campustrade.android.model.enums.ProductStatus
import java.math.BigDecimal

data class Product(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val category: ProductCategory = ProductCategory.OTHER,
    val images: List<String> = emptyList(),
    val sellerId: Long = 0,
    val seller: User? = null,
    val status: ProductStatus = ProductStatus.ON_SALE,
    val auditStatus: AuditStatus = AuditStatus.PENDING,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val contactInfo: String? = null,
    val location: String? = null,
    val remark: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)