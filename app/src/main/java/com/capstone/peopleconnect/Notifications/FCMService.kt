package com.capstone.peopleconnect.Notifications

class FCMService {
}

//  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

//package com.capstone.peopleconnect.Notifications
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import com.capstone.peopleconnect.Message.chat.ChatActivity
//import com.capstone.peopleconnect.Notifications.model.NotificationModel
//import com.capstone.peopleconnect.R
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//import com.google.firebase.database.FirebaseDatabase
//import io.getstream.chat.android.client.ChatClient
//import io.getstream.chat.android.models.Device
//import io.getstream.chat.android.models.PushProvider
//
//class FCMService : FirebaseMessagingService() {
//
//    private lateinit var database: FirebaseDatabase
//
//    override fun onCreate() {
//        super.onCreate()
//        database = FirebaseDatabase.getInstance()
//    }
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//
//        try {
//            // Register the new token with Stream Chat
//            val device = Device(
//                token = token,
//                pushProvider = PushProvider.FIREBASE,
//                providerName = "firebase"
//            )
//            ChatClient.instance().addDevice(device)
//            Log.d("FCMService", "New token registered with Stream: $token")
//
//            // Also store the token in Firebase
//            val currentUser = FirebaseAuth.getInstance().currentUser
//            currentUser?.let { user ->
//                val database = FirebaseDatabase.getInstance()
//                val userRef = database.getReference("users").child(user.uid)
//                userRef.child("fcmToken").setValue(token)
//                Log.d("FCMService", "New token stored in Firebase: $token")
//            }
//        } catch (e: Exception) {
//            Log.e("FCMService", "Error registering new token", e)
//        }
//    }
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//        Log.d("FCMService", "Message received from: ${remoteMessage.from}")
//
//        try {
//            val currentUser = FirebaseAuth.getInstance().currentUser
//            if (currentUser == null) {
//                Log.e("FCMService", "No user logged in")
//                return
//            }
//
//            // Handle Stream Chat messages
//            val streamMessage = remoteMessage.data["stream"]
//            if (streamMessage != null) {
//                handleStreamChatMessage(remoteMessage.data, currentUser.uid)
//                return
//            }
//
//            // Handle custom notifications
//            if (remoteMessage.data.isNotEmpty()) {
//                val title = remoteMessage.data["title"] ?: "New Message"
//                val message = remoteMessage.data["message"] ?: "You have a new message"
//                val senderId = remoteMessage.data["senderId"]
//                val senderName = remoteMessage.data["senderName"]
//                val type = remoteMessage.data["type"] ?: "chat"
//                val channelId = remoteMessage.data["channelId"]
//
//                // Create and store notification
//                createNotification(
//                    userId = currentUser.uid,
//                    title = title,
//                    description = message,
//                    type = type,
//                    senderId = senderId ?: "",
//                    senderName = senderName ?: "",
//                    channelId = channelId
//                )
//
//                // Show the notification
//                sendNotification(title, message, senderId, senderName)
//            }
//        } catch (e: Exception) {
//            Log.e("FCMService", "Error handling push message", e)
//        }
//    }
//
//    private fun handleStreamChatMessage(data: Map<String, String>, currentUserId: String) {
//        val message = data["message"] ?: return
//        val senderId = data["senderId"] ?: return
//        val senderName = data["senderName"] ?: "User"
//        val channelId = data["channelId"]
//
//        createNotification(
//            userId = currentUserId,
//            title = "New Message from $senderName",
//            description = message,
//            type = "chat",
//            senderId = senderId,
//            senderName = senderName,
//            channelId = channelId
//        )
//    }
//
//    private fun createNotification(
//        userId: String,
//        title: String,
//        description: String,
//        type: String,
//        senderId: String,
//        senderName: String,
//        channelId: String?
//    ) {
//        val notification = NotificationModel(
//            id = database.reference.push().key ?: return,
//            title = title,
//            description = description,
//            type = type,
//            senderId = senderId,
//            senderName = senderName,
//            timestamp = System.currentTimeMillis(),
//            isRead = false,
//            channelId = channelId
//        )
//
//        // Store notification in Firebase
//        database.reference
//            .child("notifications")
//            .child(userId)
//            .child(notification.id)
//            .setValue(notification)
//            .addOnSuccessListener {
//                Log.d("FCMService", "Notification stored successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.e("FCMService", "Error storing notification", e)
//            }
//    }
//
//    private fun sendNotification(title: String, message: String, senderId: String?, senderName: String?) {
//        val channelId = "chat_messages"
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // Create notification channel for Android O and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Chat Messages",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Notifications for new chat messages"
//                enableLights(true)
//                enableVibration(true)
//            }
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        // Create intent for notification click
//        val intent = Intent(this, ChatActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            putExtra("userId", senderId)
//            putExtra("name", senderName)
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent,
//            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Build notification
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle(title)
//            .setContentText(message)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
//
//        // Show notification
//        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
//    }
//}