package com.campustrade.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.repository.UserRepository
import com.campustrade.android.util.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = userRepository.login(username, password)
                if (response.isSuccess) {
                    // 保存用户信息和登录状态
                    response.data?.let { user ->
                        sharedPreferencesHelper.saveUserInfo(user)
                        sharedPreferencesHelper.setLoggedIn(true)
                        sharedPreferencesHelper.setUserType("user") // 标记为普通用户
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        isAdmin = false,
                        user = response.data
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
    
    fun adminLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = userRepository.adminLogin(username, password)
                if (response.isSuccess) {
                    // 保存管理员信息和登录状态
                    response.data?.let { admin ->
                        sharedPreferencesHelper.saveAdminInfo(admin)
                        sharedPreferencesHelper.setLoggedIn(true)
                        sharedPreferencesHelper.setUserType("admin") // 标记为管理员
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        isAdmin = true,
                        user = null
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
    
    fun onUsernameChanged(username: String) {
        // 可以在这里添加用户名变化的逻辑
    }
    
    fun onPasswordChanged(password: String) {
        // 可以在这里添加密码变化的逻辑
    }
    
    fun isUserLoggedIn(): Boolean {
        return sharedPreferencesHelper.isLoggedIn()
    }
    
    fun isAdmin(): Boolean {
        return sharedPreferencesHelper.getUserType() == "admin"
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val error: String? = null,
    val user: com.campustrade.android.model.User? = null
)