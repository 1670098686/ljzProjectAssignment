package com.campustrade.android.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.model.Order
import com.campustrade.android.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    fun loadOrders(page: Int = 0, status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = orderRepository.getOrders(page, 20, status)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = response.data ?: emptyList(),
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
    
    fun loadOrderDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = orderRepository.getOrderById(id)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedOrder = response.data
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
    
    fun createOrder(productId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                // 这里需要先获取商品信息，然后创建订单
                // 暂时使用模拟数据
                val response = orderRepository.createOrder(
                    Order(
                        productId = productId,
                        quantity = quantity,
                        totalAmount = BigDecimal.ZERO // 实际应该从商品价格计算
                    )
                )
                
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "创建订单失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }
    
    fun payOrder(orderId: Long, paymentMethod: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val response = orderRepository.payOrder(orderId, paymentMethod)
                
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success
                    // 重新加载订单列表
                    loadOrders()
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
    
    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val response = orderRepository.cancelOrder(orderId)
                
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success
                    // 重新加载订单列表
                    loadOrders()
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "取消订单失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }
    
    fun confirmReceipt(orderId: Long) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val response = orderRepository.confirmReceipt(orderId)
                
                if (response.isSuccess) {
                    _operationState.value = OperationState.Success
                    // 重新加载订单列表
                    loadOrders()
                } else {
                    _operationState.value = OperationState.Error(
                        response.message ?: "确认收货失败"
                    )
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val error: String? = null,
    val hasMore: Boolean = false
)

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    object Success : OperationState()
    data class Error(val message: String) : OperationState()
}