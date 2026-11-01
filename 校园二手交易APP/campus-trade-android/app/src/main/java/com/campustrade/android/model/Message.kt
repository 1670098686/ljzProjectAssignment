package com.campustrade.android.model

import java.time.LocalDateTime

data class Message(
    val id: Long = 0L,
    val conversationId: Long = 0L,          // 会话ID
    val senderId: Long = 0L,               // 发送者ID
    val sender: User? = null,              // 发送者信息
    val receiverId: Long = 0L,             // 接收者ID
    val receiver: User? = null,            // 接收者信息
    val content: String = "",              // 消息内容
    val messageType: MessageType = MessageType.TEXT, // 消息类型
    val productId: Long? = null,           // 关联商品ID（可选）
    val product: Product? = null,          // 关联商品信息（可选）
    val isRead: Boolean = false,           // 是否已读
    val createTime: LocalDateTime = LocalDateTime.now(), // 发送时间
    val updateTime: LocalDateTime = LocalDateTime.now()  // 更新时间
)

enum class MessageType(val displayName: String) {
    TEXT("文本"),
    IMAGE("图片"),
    SYSTEM("系统消息"),
    PRODUCT_LINK("商品链接");

    companion object {
        fun fromString(value: String): MessageType {
            return values().find { it.name == value } ?: TEXT
        }
    }
}