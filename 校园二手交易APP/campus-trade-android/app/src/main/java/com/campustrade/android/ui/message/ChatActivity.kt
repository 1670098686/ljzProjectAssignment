package com.campustrade.android.ui.message

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ActivityChatBinding
import com.campustrade.android.manager.PushNotificationManager
import com.campustrade.android.model.Message
import com.campustrade.android.ui.message.adapter.MessageAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private var conversationId: Long = -1L
    
    @Inject
    lateinit var pushNotificationManager: PushNotificationManager
    
    private lateinit var uiUpdateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
        if (conversationId == -1L) {
            Toast.makeText(this, "会话ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupObservers()
        setupPushNotifications()
        loadMessages()
        markMessagesAsRead()
    }

    private fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupSendButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    postDelayed({
                        smoothScrollToPosition(messageAdapter.itemCount - 1)
                    }, 100)
                }
            }
        }
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val messageContent = binding.etMessage.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(messageContent)
                binding.etMessage.setText("")
            } else {
                Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
            }
        }

        // 监听输入框回车键
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val messageContent = binding.etMessage.text.toString().trim()
                if (messageContent.isNotEmpty()) {
                    sendMessage(messageContent)
                    binding.etMessage.setText("")
                }
                true
            } else {
                false
            }
        }
    }

    private fun loadMessages() {
        viewModel.loadMessages(conversationId)
    }

    private fun markMessagesAsRead() {
        viewModel.markMessagesAsRead(conversationId)
    }

    private fun sendMessage(content: String) {
        // 这里需要获取接收者的ID，暂时使用默认值
        val receiverId = 2L
        viewModel.sendMessage(receiverId, content)
    }

    private fun setupPushNotifications() {
        // 注册UI更新接收器
        uiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { handleUIUpdate(it) }
            }
        }
        
        val filter = IntentFilter("com.campustrade.android.UI_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(uiUpdateReceiver, filter)
    }
    
    private fun handleUIUpdate(intent: Intent) {
        val updateType = intent.getStringExtra("update_type") ?: return
        
        when (updateType) {
            "NEW_MESSAGE" -> {
                val messageId = intent.getLongExtra("message_id", -1L)
                val conversationId = intent.getLongExtra("conversation_id", -1L)
                val content = intent.getStringExtra("content") ?: ""
                val senderId = intent.getLongExtra("sender_id", -1L)
                
                // 如果是当前会话的新消息，更新UI
                if (conversationId == this.conversationId) {
                    CoroutineScope(Dispatchers.Main).launch {
                        // 重新加载消息列表
                        viewModel.loadMessages(conversationId)
                        // 显示新消息提示
                        Toast.makeText(this@ChatActivity, "收到新消息", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "MESSAGES_READ" -> {
                val messageIdsJson = intent.getStringExtra("message_ids") ?: ""
                // 处理消息已读状态更新
                updateMessagesReadStatus(messageIdsJson)
            }
        }
    }
    
    private fun updateMessagesReadStatus(messageIdsJson: String) {
        try {
            // 解析消息ID列表并更新UI
            // 这里可以优化为只更新特定消息的已读状态
            viewModel.loadMessages(conversationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = if (state.isLoadingMessages) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvMessages.visibility = if (state.isLoadingMessages || state.error != null) android.view.View.GONE else android.view.View.VISIBLE
            binding.emptyState.visibility = if (state.currentMessages.isEmpty() && !state.isLoadingMessages && state.error == null) android.view.View.VISIBLE else android.view.View.GONE

            if (!state.isLoadingMessages && state.error == null) {
                messageAdapter.submitList(state.currentMessages)
                // 滚动到底部
                binding.rvMessages.postDelayed({
                    binding.rvMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }, 100)
            }

            if (state.error != null) {
                Toast.makeText(this, "加载失败: ${state.error}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.operationState.observe(this) { state ->
            when (state) {
                is OperationState.Loading -> {
                    binding.btnSend.isEnabled = false
                }
                is OperationState.Success -> {
                    binding.btnSend.isEnabled = true
                    Toast.makeText(this, "发送成功", Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                is OperationState.Error -> {
                    binding.btnSend.isEnabled = true
                    Toast.makeText(this, "发送失败: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                else -> {
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销广播接收器
        if (::uiUpdateReceiver.isInitialized) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uiUpdateReceiver)
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

        fun start(activity: android.app.Activity, conversationId: Long) {
            val intent = android.content.Intent(activity, ChatActivity::class.java)
            intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)
            activity.startActivity(intent)
        }
    }
}