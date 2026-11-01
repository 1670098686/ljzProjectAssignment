package com.campustrade.android.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campustrade.android.model.Conversation
import com.campustrade.android.model.Message
import com.campustrade.android.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val conversations = messageRepository.getConversations()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    conversations = conversations,
                    error = if (conversations.isEmpty()) "暂无会话" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMessages = true, error = null)
            try {
                val messages = messageRepository.getMessages(conversationId)
                _uiState.value = _uiState.value.copy(
                    isLoadingMessages = false,
                    currentMessages = messages,
                    error = if (messages.isEmpty()) "暂无消息" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMessages = false,
                    error = "加载消息失败: ${e.message}"
                )
            }
        }
    }

    fun sendMessage(
        receiverId: Long,
        content: String,
        messageType: String = "TEXT",
        productId: Long? = null
    ) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val message = messageRepository.sendMessage(receiverId, content, messageType, productId)
                if (message != null) {
                    _operationState.value = OperationState.Success
                    // 重新加载消息
                    loadMessages(message.conversationId)
                } else {
                    _operationState.value = OperationState.Error("发送失败")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("发送失败: ${e.message}")
            }
        }
    }

    fun markMessagesAsRead(conversationId: Long) {
        viewModelScope.launch {
            try {
                messageRepository.markMessagesAsRead(conversationId)
                // 重新加载会话列表
                loadConversations()
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    fun createConversation(participantId: Long, productId: Long? = null) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val conversation = messageRepository.createConversation(participantId, productId)
                if (conversation != null) {
                    _operationState.value = OperationState.Success
                    // 重新加载会话列表
                    loadConversations()
                } else {
                    _operationState.value = OperationState.Error("创建会话失败")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("创建会话失败: ${e.message}")
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

data class MessageUiState(
    val isLoading: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val currentMessages: List<Message> = emptyList(),
    val error: String? = null
)

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    object Success : OperationState()
    data class Error(val message: String) : OperationState()
}