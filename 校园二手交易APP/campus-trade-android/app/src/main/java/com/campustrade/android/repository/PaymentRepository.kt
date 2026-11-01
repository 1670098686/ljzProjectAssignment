package com.campustrade.android.repository

import com.campustrade.android.model.ApiResponse
import com.campustrade.android.ui.payment.model.PaymentData
import com.campustrade.android.ui.payment.model.PaymentRequest
import com.campustrade.android.ui.payment.model.PaymentResult
import com.campustrade.android.ui.payment.model.RefundRequest
import com.campustrade.android.ui.payment.model.RefundResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val apiService: com.campustrade.android.network.ApiService
) {

    /**
     * 创建支付订单并立即支付成功（课题设计简化版）
     */
    suspend fun createPayment(request: PaymentRequest): ApiResponse<PaymentData> {
        // 课题设计：直接创建支付成功的订单
        return ApiResponse(
            success = true,
            data = PaymentData(
                paymentId = "PAY_${System.currentTimeMillis()}",
                orderId = request.orderId,
                amount = request.amount,
                paymentMethod = request.paymentMethod,
                status = com.campustrade.android.ui.payment.model.PaymentStatus.SUCCESS,
                createTime = java.time.LocalDateTime.now(),
                updateTime = java.time.LocalDateTime.now(),
                transactionId = "TXN_${System.currentTimeMillis()}",
                subject = request.subject,
                body = request.body
            ),
            message = "支付成功"
        )
    }

    /**
     * 执行支付（课题设计简化版）
     */
    suspend fun executePayment(paymentId: String): ApiResponse<PaymentResult> {
        // 课题设计：直接返回支付成功
        return ApiResponse(
            success = true,
            data = PaymentResult(
                success = true,
                paymentId = paymentId,
                transactionId = "TXN_${System.currentTimeMillis()}",
                message = "支付成功"
            ),
            message = "支付成功"
        )
    }

    /**
     * 查询支付状态
     */
    suspend fun getPaymentStatus(paymentId: String): ApiResponse<PaymentData> {
        // 模拟支付状态查询
        return ApiResponse(
            success = true,
            data = PaymentData(
                paymentId = paymentId,
                orderId = 1L,
                amount = 100.0.toBigDecimal(),
                paymentMethod = com.campustrade.android.ui.payment.model.PaymentMethod.ALIPAY,
                status = com.campustrade.android.ui.payment.model.PaymentStatus.SUCCESS,
                createTime = java.time.LocalDateTime.now().minusMinutes(5),
                updateTime = java.time.LocalDateTime.now().minusMinutes(4),
                transactionId = "TXN_${System.currentTimeMillis()}"
            ),
            message = "查询成功"
        )
    }

    /**
     * 申请退款
     */
    suspend fun requestRefund(request: RefundRequest): ApiResponse<RefundResult> {
        // 模拟退款申请
        return ApiResponse(
            success = true,
            data = RefundResult(
                success = true,
                refundId = "REF_${System.currentTimeMillis()}",
                message = "退款申请已提交"
            ),
            message = "退款申请已提交"
        )
    }

    /**
     * 取消支付
     */
    suspend fun cancelPayment(paymentId: String): ApiResponse<Unit> {
        // 模拟取消支付
        return ApiResponse(
            success = true,
            message = "支付已取消"
        )
    }

    /**
     * 获取支付历史
     */
    suspend fun getPaymentHistory(userId: Long, page: Int = 0, size: Int = 20): ApiResponse<List<PaymentData>> {
        // 模拟支付历史
        val payments = listOf(
            PaymentData(
                paymentId = "PAY_001",
                orderId = 1L,
                amount = 3500.0.toBigDecimal(),
                paymentMethod = com.campustrade.android.ui.payment.model.PaymentMethod.ALIPAY,
                status = com.campustrade.android.ui.payment.model.PaymentStatus.SUCCESS,
                createTime = java.time.LocalDateTime.now().minusDays(2),
                updateTime = java.time.LocalDateTime.now().minusDays(2),
                transactionId = "TXN_001"
            ),
            PaymentData(
                paymentId = "PAY_002",
                orderId = 2L,
                amount = 80.0.toBigDecimal(),
                paymentMethod = com.campustrade.android.ui.payment.model.PaymentMethod.WECHAT,
                status = com.campustrade.android.ui.payment.model.PaymentStatus.SUCCESS,
                createTime = java.time.LocalDateTime.now().minusDays(1),
                updateTime = java.time.LocalDateTime.now().minusDays(1),
                transactionId = "TXN_002"
            )
        )
        
        return ApiResponse(
            success = true,
            data = payments,
            message = "获取成功"
        )
    }
}

// 扩展PaymentData类以支持模拟数据
private data class PaymentData(
    val paymentId: String,
    val orderId: Long,
    val amount: java.math.BigDecimal,
    val paymentMethod: com.campustrade.android.ui.payment.model.PaymentMethod,
    val status: com.campustrade.android.ui.payment.model.PaymentStatus,
    val createTime: java.time.LocalDateTime,
    val updateTime: java.time.LocalDateTime? = null,
    val transactionId: String? = null,
    val subject: String? = null,
    val body: String? = null
)