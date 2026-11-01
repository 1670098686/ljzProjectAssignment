package com.campustrade.android.ui.message.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ItemConversationBinding
import com.campustrade.android.model.Conversation
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationAdapter(
    private val onItemClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.bind(conversation)
    }

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(conversation: Conversation) {
            binding.apply {
                // 设置对方用户信息
                val otherUser = if (conversation.participant1?.id == getCurrentUserId()) {
                    conversation.participant2
                } else {
                    conversation.participant1
                }
                
                otherUser?.let { user ->
                    tvUsername.text = user.username
                    // 这里应该加载用户头像，暂时使用占位图
                    // Glide.with(itemView).load(user.avatar).into(ivAvatar)
                }
                
                // 设置商品信息
                conversation.product?.let { product ->
                    tvProductTitle.text = product.title
                    tvProductTitle.visibility = android.view.View.VISIBLE
                } ?: run {
                    tvProductTitle.visibility = android.view.View.GONE
                }
                
                // 设置最后一条消息
                conversation.lastMessage?.let { message ->
                    tvLastMessage.text = when (message.messageType) {
                        com.campustrade.android.model.MessageType.IMAGE -> "[图片]"
                        com.campustrade.android.model.MessageType.SYSTEM -> "[系统消息]"
                        com.campustrade.android.model.MessageType.PRODUCT_LINK -> "[商品链接]"
                        else -> message.content
                    }
                    tvTime.text = formatTime(message.createTime)
                } ?: run {
                    tvLastMessage.text = "暂无消息"
                    tvTime.text = ""
                }
                
                // 设置未读消息数
                if (conversation.unreadCount > 0) {
                    tvUnreadCount.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
                    tvUnreadCount.visibility = android.view.View.VISIBLE
                } else {
                    tvUnreadCount.visibility = android.view.View.GONE
                }
                
                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(conversation)
                }
            }
        }
        
        private fun getCurrentUserId(): Long {
            // 这里应该从用户管理器中获取当前用户ID
            // 暂时返回一个默认值
            return 1L
        }
        
        private fun formatTime(date: java.time.LocalDateTime): String {
            val now = java.time.LocalDateTime.now()
            val today = java.time.LocalDate.now()
            val messageDate = date.toLocalDate()
            
            return if (messageDate == today) {
                // 今天：显示时间
                val formatter = SimpleDateFormat("HH:mm", Locale.CHINA)
                formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
            } else if (messageDate == today.minusDays(1)) {
                // 昨天：显示"昨天"
                "昨天"
            } else if (messageDate.isAfter(today.minusDays(7))) {
                // 一周内：显示星期几
                val formatter = SimpleDateFormat("EEEE", Locale.CHINA)
                formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
            } else {
                // 更早：显示日期
                val formatter = SimpleDateFormat("MM/dd", Locale.CHINA)
                formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
            }
        }
    }

    companion object {
        private val ConversationDiffCallback = object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
                return oldItem == newItem
            }
        }
    }
}