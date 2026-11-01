package com.campustrade.android.ui.payment.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 支付数据模型
 */
data class PaymentData(
    val paymentId: String,
    val orderId: Long,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime? = null,
    val transactionId: String? = null
)

/**
 * 支付方式枚举
 */
enum class PaymentMethod {
    SIMULATED  // 模拟支付（课题设计使用）
}

/**
 * 支付状态枚举
 */
enum class PaymentStatus {
    PENDING,      // 待支付
    PROCESSING,   // 处理中
    SUCCESS,      // 支付成功
    FAILED,       // 支付失败
    CANCELLED,    // 已取消
    REFUNDED      // 已退款
}

/**
 * 支付请求参数
 */
data class PaymentRequest(
    val orderId: Long,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val subject: String,
    val body: String? = null
)

/**
 * 支付结果
 */
data class PaymentResult(
    val success: Boolean,
    val paymentId: String,
    val transactionId: String? = null,
    val message: String? = null,
    val errorCode: String? = null
)

/**
 * 退款请求参数
 */
data class RefundRequest(
    val paymentId: String,
    val amount: BigDecimal,
    val reason: String
)

/**
 * 退款结果
 */
data class RefundResult(
    val success: Boolean,
    val refundId: String,
    val message: String? = null,
    val errorCode: String? = null
)