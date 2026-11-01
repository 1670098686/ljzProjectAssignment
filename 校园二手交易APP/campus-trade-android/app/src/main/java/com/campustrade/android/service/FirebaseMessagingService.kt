package com.campustrade.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.campustrade.android.R
import com.campustrade.android.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "campus_trade_notifications"
        const val CHANNEL_NAME = "校园二手交易通知"
        const val NOTIFICATION_ID = 1002
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 将新的FCM token发送到服务器
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // 处理接收到的消息
        handleMessage(remoteMessage)
    }

    private fun handleMessage(remoteMessage: RemoteMessage) {
        // 检查消息是否包含数据负载
        remoteMessage.data.isNotEmpty().let { 
            val title = remoteMessage.data["title"] ?: "校园二手交易"
            val message = remoteMessage.data["message"] ?: "您有新的消息"
            val type = remoteMessage.data["type"] ?: "general"
            val targetId = remoteMessage.data["targetId"]
            
            // 根据消息类型处理
            when (type) {
                "new_message" -> {
                    showNotification(title, message, type, targetId)
                    // 发送广播更新UI
                    sendMessageBroadcast(type, targetId)
                }
                "order_update" -> {
                    showNotification(title, message, type, targetId)
                    sendMessageBroadcast(type, targetId)
                }
                "system_notification" -> {
                    showNotification(title, message, type, targetId)
                    sendMessageBroadcast(type, targetId)
                }
                else -> {
                    showNotification(title, message, type, targetId)
                }
            }
        }
        
        // 检查是否包含通知负载（前台显示）
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "校园二手交易"
            val body = notification.body ?: "您有新的消息"
            showNotification(title, body, "general", null)
        }
    }

    private fun showNotification(title: String, message: String, type: String, targetId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "校园二手交易应用通知"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建点击通知的Intent
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            targetId?.let { putExtra("target_id", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建通知声音
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // 构建通知
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        // 根据消息类型设置不同的样式
        when (type) {
            "new_message" -> {
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
            "order_update" -> {
                notificationBuilder.setStyle(NotificationCompat.InboxStyle()
                    .addLine(message))
            }
            else -> {
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
        }
        
        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun sendMessageBroadcast(type: String, targetId: String?) {
        val intent = Intent("com.campustrade.android.FCM_MESSAGE")
        intent.putExtra("type", type)
        targetId?.let { intent.putExtra("target_id", it) }
        sendBroadcast(intent)
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 这里应该调用API将token发送到服务器
                // 例如：apiService.updateFcmToken(token)
                // 暂时先记录日志
                android.util.Log.d("FCM", "New token: $token")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}