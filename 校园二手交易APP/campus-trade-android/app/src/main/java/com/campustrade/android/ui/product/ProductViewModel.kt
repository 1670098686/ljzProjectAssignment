package com.campustrade.android.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.model.Product
import com.campustrade.android.model.enums.ProductCategory
import com.campustrade.android.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()
    
    private val _publishState = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publishState.asStateFlow()
    
    fun loadProducts(page: Int = 0, category: String? = null, keyword: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = productRepository.getProducts(page, 20, category, keyword)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = response.data ?: emptyList(),
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

    fun loadMyProducts(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 这里应该从用户管理器中获取当前用户ID
                val currentUserId = 1L
                val response = productRepository.getMyProducts(currentUserId, page, 20)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = response.data ?: emptyList(),
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

    fun loadFavoriteProducts(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 这里应该从用户管理器中获取当前用户ID
                val currentUserId = 1L
                val response = productRepository.getFavoriteProducts(currentUserId, page, 20)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = response.data ?: emptyList(),
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
    
    fun loadProductDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = productRepository.getProductById(id)
                if (response.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedProduct = response.data
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun publishProduct(
        title: String,
        price: BigDecimal,
        description: String,
        category: ProductCategory,
        contactInfo: String,
        location: String,
        images: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _publishState.value = PublishState.Loading
            try {
                val response = productRepository.publishProduct(
                    title = title,
                    price = price,
                    description = description,
                    category = category,
                    contactInfo = contactInfo,
                    location = location,
                    images = images
                )
                
                if (response.isSuccess) {
                    _publishState.value = PublishState.Success
                } else {
                    _publishState.value = PublishState.Error(
                        response.message ?: "发布失败，请稍后重试"
                    )
                }
            } catch (e: Exception) {
                _publishState.value = PublishState.Error(
                    "网络连接失败: ${e.message}"
                )
            }
        }
    }
    
    fun resetPublishState() {
        _publishState.value = PublishState.Idle
    }
}

data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val error: String? = null,
    val hasMore: Boolean = false
)

sealed class PublishState {
    object Idle : PublishState()
    object Loading : PublishState()
    object Success : PublishState()
    data class Error(val message: String) : PublishState()
}