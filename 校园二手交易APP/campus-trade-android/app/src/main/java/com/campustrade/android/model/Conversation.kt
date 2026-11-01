package com.campustrade.android.model

import java.time.LocalDateTime

data class Conversation(
    val id: Long = 0L,
    val participant1Id: Long = 0L,         // 参与者1 ID
    val participant1: User? = null,        // 参与者1信息
    val participant2Id: Long = 0L,         // 参与者2 ID
    val participant2: User? = null,        // 参与者2信息
    val productId: Long? = null,           // 关联商品ID（可选）
    val product: Product? = null,          // 关联商品信息（可选）
    val lastMessage: Message? = null,      // 最后一条消息
    val unreadCount: Int = 0,              // 未读消息数
    val createTime: LocalDateTime = LocalDateTime.now(), // 创建时间
    val updateTime: LocalDateTime = LocalDateTime.now()  // 更新时间
)

data class ConversationWithMessages(
    val conversation: Conversation,
    val messages: List<Message> = emptyList()
)