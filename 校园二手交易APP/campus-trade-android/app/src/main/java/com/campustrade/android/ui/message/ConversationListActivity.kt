package com.campustrade.android.ui.message

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.campustrade.android.R
import com.campustrade.android.databinding.ActivityConversationListBinding
import com.campustrade.android.manager.PushNotificationManager
import com.campustrade.android.ui.message.adapter.ConversationAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConversationListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationListBinding
    private val viewModel: MessageViewModel by viewModels()
    private lateinit var conversationAdapter: ConversationAdapter
    
    @Inject
    lateinit var pushNotificationManager: PushNotificationManager
    
    private lateinit var uiUpdateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupPushNotifications()
        loadConversations()
    }

    private fun setupUI() {
        setupToolbar()
        setupRecyclerView()
        setupRetryButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            // 点击会话项，跳转到聊天页面
            ChatActivity.start(this, conversation.id)
        }

        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@ConversationListActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            loadConversations()
        }
    }

    private fun loadConversations() {
        viewModel.loadConversations()
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
                
                // 收到新消息，刷新会话列表
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.loadConversations()
                    // 显示新消息提示
                    Toast.makeText(this@ConversationListActivity, "收到新消息", Toast.LENGTH_SHORT).show()
                }
            }
            "CONVERSATION_UPDATED" -> {
                // 会话更新，刷新列表
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.loadConversations()
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                binding.rvConversations.visibility = if (state.isLoading || state.error != null) android.view.View.GONE else android.view.View.VISIBLE
                binding.emptyState.visibility = if (state.conversations.isEmpty() && !state.isLoading && state.error == null) android.view.View.VISIBLE else android.view.View.GONE
                binding.errorState.visibility = if (state.error != null) android.view.View.VISIBLE else android.view.View.GONE

                if (state.error != null) {
                    binding.tvErrorMessage.text = state.error
                }

                if (!state.isLoading && state.error == null) {
                    conversationAdapter.submitList(state.conversations)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.operationState.collect { state ->
                when (state) {
                    is OperationState.Loading -> {
                        // 显示加载状态
                    }
                    is OperationState.Success -> {
                        Toast.makeText(this@ConversationListActivity, "操作成功", Toast.LENGTH_SHORT).show()
                        viewModel.resetOperationState()
                    }
                    is OperationState.Error -> {
                        Toast.makeText(this@ConversationListActivity, "操作失败: ${state.message}", Toast.LENGTH_SHORT).show()
                        viewModel.resetOperationState()
                    }
                    else -> {}
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
        fun start(activity: android.app.Activity) {
            val intent = android.content.Intent(activity, ConversationListActivity::class.java)
            activity.startActivity(intent)
        }
    }
}