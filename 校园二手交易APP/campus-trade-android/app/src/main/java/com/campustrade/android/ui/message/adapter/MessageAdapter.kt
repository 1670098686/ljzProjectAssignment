package com.campustrade.android.ui.message.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ItemMessageReceivedBinding
import com.campustrade.android.databinding.ItemMessageSentBinding
import com.campustrade.android.model.Message

class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (isCurrentUserMessage(message)) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessageContent.text = when (message.messageType) {
                com.campustrade.android.model.MessageType.IMAGE -> "[图片]"
                com.campustrade.android.model.MessageType.SYSTEM -> "[系统消息]"
                com.campustrade.android.model.MessageType.PRODUCT_LINK -> "[商品链接]"
                else -> message.content
            }

            // 设置发送者头像
            message.sender?.let { sender ->
                // 这里应该加载用户头像，暂时使用占位图
                // Glide.with(itemView).load(sender.avatar).into(binding.ivAvatar)
            }
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessageContent.text = when (message.messageType) {
                com.campustrade.android.model.MessageType.IMAGE -> "[图片]"
                com.campustrade.android.model.MessageType.SYSTEM -> "[系统消息]"
                com.campustrade.android.model.MessageType.PRODUCT_LINK -> "[商品链接]"
                else -> message.content
            }

            // 设置发送者头像
            message.sender?.let { sender ->
                // 这里应该加载用户头像，暂时使用占位图
                // Glide.with(itemView).load(sender.avatar).into(binding.ivAvatar)
            }
        }
    }

    private fun isCurrentUserMessage(message: Message): Boolean {
        // 这里应该从用户管理器中获取当前用户ID
        // 暂时返回一个默认值
        val currentUserId = 1L
        return message.senderId == currentUserId
    }

    companion object {
        private val MessageDiffCallback = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }
    }
}