package com.campustrade.android.repository

import com.campustrade.android.network.ApiService
import com.campustrade.android.model.Conversation
import com.campustrade.android.model.Message
import com.campustrade.android.model.ConversationWithMessages
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getConversations(
        page: Int = 0,
        size: Int = 20
    ): List<Conversation> {
        return try {
            val response = apiService.getConversations(page, size)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.content
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getConversationDetail(
        conversationId: Long
    ): ConversationWithMessages? {
        return try {
            val response = apiService.getConversationDetail(conversationId)
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMessages(
        conversationId: Long,
        page: Int = 0,
        size: Int = 50
    ): List<Message> {
        return try {
            val response = apiService.getMessages(conversationId, page, size)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.content
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendMessage(
        receiverId: Long,
        content: String,
        messageType: String = "TEXT",
        productId: Long? = null
    ): Message? {
        return try {
            val request = mapOf(
                "receiverId" to receiverId,
                "content" to content,
                "messageType" to messageType,
                "productId" to productId
            )
            val response = apiService.sendMessage(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun markMessagesAsRead(conversationId: Long): Boolean {
        return try {
            val response = apiService.markMessagesAsRead(conversationId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createConversation(
        participantId: Long,
        productId: Long? = null
    ): Conversation? {
        return try {
            val request = mapOf(
                "participantId" to participantId,
                "productId" to productId
            )
            val response = apiService.createConversation(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteConversation(conversationId: Long): Boolean {
        return try {
            val response = apiService.deleteConversation(conversationId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}