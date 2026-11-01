package com.campustrade.android.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.campustrade.android.service.WebSocketService
import com.campustrade.android.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var isWebSocketServiceRunning = false
    
    fun initialize() {
        startWebSocketService()
        registerBroadcastReceivers()
    }
    
    private fun startWebSocketService() {
        if (isWebSocketServiceRunning) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val intent = Intent(context, WebSocketService::class.java)
                context.startService(intent)
                isWebSocketServiceRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun registerBroadcastReceivers() {
        // 注册WebSocket事件接收器
        val webSocketFilter = IntentFilter("com.campustrade.android.WEBSOCKET_EVENT")
        LocalBroadcastManager.getInstance(context).registerReceiver(
            webSocketReceiver,
            webSocketFilter
        )
        
        // 注册FCM消息接收器
        val fcmFilter = IntentFilter("com.campustrade.android.FCM_MESSAGE")
        LocalBroadcastManager.getInstance(context).registerReceiver(
            fcmReceiver,
            fcmFilter
        )
    }
    
    private val webSocketReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val eventType = it.getStringExtra("event_type") ?: return
                val data = it.getStringExtra("data") ?: return
                
                handleWebSocketEvent(eventType, data)
            }
        }
    }
    
    private val fcmReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val type = it.getStringExtra("type") ?: return
                val targetId = it.getStringExtra("target_id")
                
                handleFcmMessage(type, targetId)
            }
        }
    }
    
    private fun handleWebSocketEvent(eventType: String, data: String) {
        when (eventType) {
            "NEW_MESSAGE" -> {
                // 处理新消息
                handleNewMessage(data)
            }
            "MESSAGES_READ" -> {
                // 处理消息已读状态
                handleMessagesRead(data)
            }
            "NEW_NOTIFICATION" -> {
                // 处理新通知
                handleNewNotification(data)
            }
            "NOTIFICATION_READ" -> {
                // 处理通知已读状态
                handleNotificationRead(data)
            }
        }
    }
    
    private fun handleFcmMessage(type: String, targetId: String?) {
        when (type) {
            "new_message" -> {
                // 离线时收到新消息推送
                broadcastToUI("FCM_NEW_MESSAGE", targetId)
            }
            "order_update" -> {
                // 订单状态更新
                broadcastToUI("FCM_ORDER_UPDATE", targetId)
            }
            "system_notification" -> {
                // 系统通知
                broadcastToUI("FCM_SYSTEM_NOTIFICATION", targetId)
            }
        }
    }
    
    private fun handleNewMessage(data: String) {
        try {
            val jsonObject = JSONObject(data)
            val messageId = jsonObject.getLong("id")
            val conversationId = jsonObject.getLong("conversationId")
            val content = jsonObject.getString("content")
            val senderId = jsonObject.getLong("senderId")
            
            // 发送广播到UI更新
            val intent = Intent("com.campustrade.android.UI_UPDATE")
            intent.putExtra("update_type", "NEW_MESSAGE")
            intent.putExtra("message_id", messageId)
            intent.putExtra("conversation_id", conversationId)
            intent.putExtra("content", content)
            intent.putExtra("sender_id", senderId)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun handleMessagesRead(data: String) {
        try {
            // 处理消息已读状态更新
            val intent = Intent("com.campustrade.android.UI_UPDATE")
            intent.putExtra("update_type", "MESSAGES_READ")
            intent.putExtra("message_ids", data)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun handleNewNotification(data: String) {
        try {
            val jsonObject = JSONObject(data)
            val notificationId = jsonObject.getLong("id")
            val title = jsonObject.getString("title")
            val content = jsonObject.getString("content")
            val type = jsonObject.getString("type")
            
            // 发送广播到UI更新
            val intent = Intent("com.campustrade.android.UI_UPDATE")
            intent.putExtra("update_type", "NEW_NOTIFICATION")
            intent.putExtra("notification_id", notificationId)
            intent.putExtra("title", title)
            intent.putExtra("content", content)
            intent.putExtra("notification_type", type)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun handleNotificationRead(data: String) {
        try {
            val notificationId = data.toLong()
            
            // 发送广播到UI更新
            val intent = Intent("com.campustrade.android.UI_UPDATE")
            intent.putExtra("update_type", "NOTIFICATION_READ")
            intent.putExtra("notification_id", notificationId)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun broadcastToUI(updateType: String, data: String? = null) {
        val intent = Intent("com.campustrade.android.UI_UPDATE")
        intent.putExtra("update_type", updateType)
        data?.let { intent.putExtra("data", it) }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
    
    fun sendMessage(messageJson: String) {
        val intent = Intent(context, WebSocketService::class.java)
        intent.putExtra("action", "send_message")
        intent.putExtra("message", messageJson)
        context.startService(intent)
    }
    
    fun markMessagesAsRead(messageIds: List<Long>) {
        val intent = Intent(context, WebSocketService::class.java)
        intent.putExtra("action", "mark_read")
        intent.putExtra("message_ids", messageIds.toLongArray())
        context.startService(intent)
    }
    
    fun disconnect() {
        val intent = Intent(context, WebSocketService::class.java)
        context.stopService(intent)
        isWebSocketServiceRunning = false
    }
    
    fun cleanup() {
        disconnect()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(webSocketReceiver)
        LocalBroadcastManager.getInstance(context).unregisterReceiver(fcmReceiver)
    }
}