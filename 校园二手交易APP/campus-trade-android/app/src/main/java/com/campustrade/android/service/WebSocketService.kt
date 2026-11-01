package com.campustrade.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.campustrade.android.R
import com.campustrade.android.util.Constants
import com.campustrade.android.util.SharedPreferencesHelper
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompMessage
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    
    private var stompClient: StompClient? = null
    private var isConnected = false
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "websocket_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectWebSocket()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("校园二手交易")
            .setContentText("实时消息推送服务运行中")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun connectWebSocket() {
        if (isConnected) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = sharedPreferencesHelper.getToken()
                if (token.isNullOrEmpty()) {
                    return@launch
                }
                
                // 创建WebSocket连接
                val serverUrl = "ws://${Constants.BASE_URL.replace("http://", "").replace("https://", "")}/ws"
                
                stompClient = Stomp.over(
                    Stomp.ConnectionProvider.OKHTTP,
                    serverUrl,
                    mapOf("Authorization" to "Bearer $token")
                )
                
                stompClient?.connect()
                
                // 订阅消息队列
                stompClient?.topic("/user/queue/messages")?.subscribe { message ->
                    handleIncomingMessage(message)
                }
                
                // 订阅通知队列
                stompClient?.topic("/user/queue/notifications")?.subscribe { message ->
                    handleNotification(message)
                }
                
                isConnected = true
                
            } catch (e: Exception) {
                e.printStackTrace()
                // 连接失败，5秒后重试
                Thread.sleep(5000)
                connectWebSocket()
            }
        }
    }

    private fun handleIncomingMessage(message: StompMessage) {
        try {
            val payload = message.payload
            val jsonObject = JSONObject(payload)
            
            when (jsonObject.getString("type")) {
                "MESSAGE_CREATED" -> {
                    // 处理新消息
                    val messageData = jsonObject.getJSONObject("message")
                    broadcastMessageEvent("NEW_MESSAGE", messageData.toString())
                }
                "MESSAGES_READ" -> {
                    // 处理消息已读状态
                    val messageIds = jsonObject.getJSONArray("messageIds")
                    broadcastMessageEvent("MESSAGES_READ", messageIds.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNotification(message: StompMessage) {
        try {
            val payload = message.payload
            val jsonObject = JSONObject(payload)
            
            when (jsonObject.getString("type")) {
                "NEW_NOTIFICATION" -> {
                    // 处理新通知
                    val notificationData = jsonObject.getJSONObject("notification")
                    broadcastMessageEvent("NEW_NOTIFICATION", notificationData.toString())
                }
                "NOTIFICATION_READ" -> {
                    // 处理通知已读状态
                    val notificationId = jsonObject.getLong("notificationId")
                    broadcastMessageEvent("NOTIFICATION_READ", notificationId.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun broadcastMessageEvent(eventType: String, data: String) {
        val intent = Intent("com.campustrade.android.WEBSOCKET_EVENT")
        intent.putExtra("event_type", eventType)
        intent.putExtra("data", data)
        sendBroadcast(intent)
    }

    fun sendMessage(messageJson: String) {
        if (!isConnected) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stompClient?.send("/app/messages/send", messageJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markMessagesAsRead(messageIds: List<Long>) {
        if (!isConnected) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("messageIds", messageIds)
                }
                stompClient?.send("/app/messages/read", jsonObject.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
    }

    private fun disconnectWebSocket() {
        try {
            stompClient?.disconnect()
            isConnected = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}