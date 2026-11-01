package com.campustrade.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.repository.ProfileRepository
import com.campustrade.android.ui.profile.model.ProfileData
import com.campustrade.android.ui.profile.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadProfileData(userId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val profileData = profileRepository.getProfileData(userId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profileData = profileData,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun updateUserInfo(userInfo: UserInfo) {
        _operationState.value = OperationState.Loading
        
        viewModelScope.launch {
            try {
                val success = profileRepository.updateUserInfo(userInfo)
                if (success) {
                    _operationState.value = OperationState.Success("更新成功")
                    // 重新加载数据
                    loadProfileData(userInfo.id)
                } else {
                    _operationState.value = OperationState.Error("更新失败")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "更新失败")
            }
        }
    }

    fun uploadAvatar(userId: Long, avatarFile: java.io.File) {
        _operationState.value = OperationState.Loading
        
        viewModelScope.launch {
            try {
                val avatarUrl = profileRepository.uploadAvatar(userId, avatarFile)
                if (avatarUrl != null) {
                    _operationState.value = OperationState.Success("头像上传成功")
                    // 重新加载数据
                    loadProfileData(userId)
                } else {
                    _operationState.value = OperationState.Error("头像上传失败")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "头像上传失败")
            }
        }
    }

    fun logout() {
        _operationState.value = OperationState.Loading
        
        viewModelScope.launch {
            try {
                val success = profileRepository.logout()
                if (success) {
                    _operationState.value = OperationState.Success("退出成功")
                } else {
                    _operationState.value = OperationState.Error("退出失败")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "退出失败")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 个人中心UI状态
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val profileData: ProfileData? = null,
    val error: String? = null
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