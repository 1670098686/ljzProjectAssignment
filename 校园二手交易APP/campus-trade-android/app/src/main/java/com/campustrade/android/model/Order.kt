package com.campustrade.android.model

import com.campustrade.android.model.enums.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
    val id: Long = 0,
    val orderNumber: String = "",
    val productId: Long = 0,
    val product: Product? = null,
    val buyerId: Long = 0,
    val buyer: User? = null,
    val sellerId: Long = 0,
    val seller: User? = null,
    val quantity: Int = 1,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: String? = null,
    val paymentTime: LocalDateTime? = null,
    val shippingAddress: String? = null,
    val contactInfo: String? = null,
    val remark: String? = null,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null
)