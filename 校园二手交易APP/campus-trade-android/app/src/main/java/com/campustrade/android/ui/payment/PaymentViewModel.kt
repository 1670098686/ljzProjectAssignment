package com.campustrade.android.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.repository.PaymentRepository
import com.campustrade.android.ui.payment.model.PaymentData
import com.campustrade.android.ui.payment.model.PaymentRequest
import com.campustrade.android.ui.payment.model.PaymentResult
import com.campustrade.android.ui.payment.model.RefundRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    /**
     * 创建支付订单
     */
    fun createPayment(orderId: Long, amount: BigDecimal, paymentMethod: com.campustrade.android.ui.payment.model.PaymentMethod, subject: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val request = PaymentRequest(
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    subject = subject
                )
                val response = paymentRepository.createPayment(request)
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success("支付订单创建成功")
                    _uiState.value = _uiState.value.copy(
                        currentPayment = response.data
                    )
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "创建支付订单失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 执行支付
     */
    fun executePayment(paymentId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val response = paymentRepository.executePayment(paymentId)
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success("支付成功")
                    // 更新支付状态
                    getPaymentStatus(paymentId)
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "支付失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 查询支付状态
     */
    fun getPaymentStatus(paymentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = paymentRepository.getPaymentStatus(paymentId)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPayment = response.data
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 申请退款
     */
    fun requestRefund(paymentId: String, amount: BigDecimal, reason: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val request = RefundRequest(
                    paymentId = paymentId,
                    amount = amount,
                    reason = reason
                )
                val response = paymentRepository.requestRefund(request)
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success("退款申请已提交")
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "退款申请失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 取消支付
     */
    fun cancelPayment(paymentId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val response = paymentRepository.cancelPayment(paymentId)
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success("支付已取消")
                    _uiState.value = _uiState.value.copy(
                        currentPayment = null
                    )
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "取消支付失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取支付历史
     */
    fun loadPaymentHistory(userId: Long, page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = paymentRepository.getPaymentHistory(userId, page, 20)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        paymentHistory = response.data ?: emptyList(),
                        hasMore = (response.data?.size ?: 0) >= 20
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网络连接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 重置操作状态
     */
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}

/**
 * 支付UI状态
 */
data class PaymentUiState(
    val isLoading: Boolean = false,
    val currentPayment: PaymentData? = null,
    val paymentHistory: List<PaymentData> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = false
)

/**
 * 操作状态
 */
sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}