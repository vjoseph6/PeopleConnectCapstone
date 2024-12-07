package com.capstone.peopleconnect.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.capstone.peopleconnect.Message.chat.ChatActivity
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Device
import io.getstream.chat.android.models.PushProvider
import com.capstone.peopleconnect.Client.ClientMainActivity
import com.capstone.peopleconnect.SPrvoider.SProviderMainActivity

class FCMService : FirebaseMessagingService() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        try {
            // Register the new token with Stream Chat
            val device = Device(
                token = token,
                pushProvider = PushProvider.FIREBASE,
                providerName = "firebase"
            )
            ChatClient.instance().addDevice(device)
            Log.d("FCMService", "New token registered with Stream: $token")

            // Also store the token in Firebase
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val database = FirebaseDatabase.getInstance()
                val userRef = database.getReference("users").child(user.uid)
                userRef.child("fcmToken").setValue(token)
                Log.d("FCMService", "New token stored in Firebase: $token")
            }
        } catch (e: Exception) {
            Log.e("FCMService", "Error registering new token", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "Message received from: ${remoteMessage.from}")

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("FCMService", "No user logged in")
                return
            }

            // Handle Stream Chat messages
            val streamMessage = remoteMessage.data["stream"]
            if (streamMessage != null) {
                handleStreamChatMessage(remoteMessage.data, currentUser.uid)
                return
            }

            // Handle custom notifications
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "New Message"
                val message = remoteMessage.data["message"] ?: "You have a new message"
                val senderId = remoteMessage.data["senderId"]
                val senderName = remoteMessage.data["senderName"]
                val type = remoteMessage.data["type"] ?: "chat"
                val channelId = remoteMessage.data["channelId"]
                val postId = remoteMessage.data["postId"]

                // Create and store notification
                createNotification(
                    userId = currentUser.uid,
                    title = title,
                    description = message,
                    type = type,
                    senderId = senderId ?: "",
                    senderName = senderName ?: "",
                    channelId = channelId,
                    postId = postId
                )

                // Show the notification with appropriate pattern
                if (type == "post_request" || type == "post_application" ||
                    type == "post_status" || type == "application_status") {
                    // Use stronger vibration + sound for post-related notifications
                    sendNotification(title, message, senderId, senderName, true)
                } else {
                    // Use normal pattern for other notifications
                    sendNotification(title, message, senderId, senderName, false)
                }
            }
        } catch (e: Exception) {
            Log.e("FCMService", "Error handling push message", e)
        }
    }

    private fun handleStreamChatMessage(data: Map<String, String>, currentUserId: String) {
        val message = data["message"] ?: return
        val senderId = data["senderId"] ?: return
        val senderName = data["senderName"] ?: "User"
        val channelId = data["channelId"]
        val postId = data["postId"]

        createNotification(
            userId = currentUserId,
            title = "New Message from $senderName",
            description = message,
            type = "chat",
            senderId = senderId,
            senderName = senderName,
            channelId = channelId,
            postId = postId
        )
    }

    private fun handleBookingNotification(
        userId: String,
        title: String,
        description: String,
        senderId: String,
        senderName: String,
        bookingId: String,
        bookingStatus: String,
        bookingDate: String,
        bookingTime: String,
        cancellationReason: String? = null
    ) {
        val notification = NotificationModel(
            id = database.reference.push().key ?: return,
            title = title,
            description = description,
            type = "booking",
            senderId = senderId,
            senderName = senderName,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            bookingId = bookingId,
            bookingStatus = bookingStatus,
            bookingDate = bookingDate,
            bookingTime = bookingTime,
            cancellationReason = cancellationReason
        )

        // Store notification in Firebase
        database.reference
            .child("notifications")
            .child(userId)
            .child(notification.id)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d("FCMService", "Booking notification stored successfully")
                // Send system notification
                sendNotification(title, description, senderId, senderName, false)
            }
            .addOnFailureListener { e ->
                Log.e("FCMService", "Error storing booking notification", e)
            }
    }

    private fun createNotification(
        userId: String,
        title: String,
        description: String,
        type: String,
        senderId: String,
        senderName: String,
        channelId: String? = null,
        postId: String? = null
    ) {
        val notification = NotificationModel(
            id = database.reference.push().key ?: return,
            title = title,
            description = description,
            type = type,
            senderId = senderId,
            senderName = senderName,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            channelId = channelId,
            postId = postId
        )

        // Store notification in Firebase
        database.reference
            .child("notifications")
            .child(userId)
            .child(notification.id)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d("FCMService", "Notification stored successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FCMService", "Error storing notification", e)
            }
    }

    private fun sendNotification(title: String, message: String, senderId: String?, senderName: String?, isPostNotification: Boolean) {
        val channelId = "chat_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Create intent based on notification type
        val intent = when {
            title.contains("Video Call") -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            title.contains("New Application") ||
                    title.contains("Post") ||
                    title.contains("Booking") ||
                    title.contains("Service Provider") -> {
                // Default to SProviderMainActivity, will be updated when user type is checked
                val defaultIntent = Intent(this, SProviderMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("OPEN_NOTIFICATIONS", true)
                }

                // Check user type asynchronously
                currentUser?.let { user ->
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(user.uid)
                        .child("userType")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val isClient = snapshot.getValue(String::class.java) == "client"
                            if (isClient) {
                                val clientIntent = Intent(this, ClientMainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("OPEN_NOTIFICATIONS", true)
                                }
                                showNotificationWithIntent(title, message, clientIntent, channelId, notificationManager)
                            }
                        }
                }
                defaultIntent
            }
            else -> {
                Intent(this, SProviderMainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("OPEN_NOTIFICATIONS", true)
                }
            }
        }

        // Show initial notification
        showNotificationWithIntent(title, message, intent, channelId, notificationManager)
    }

    private fun showNotificationWithIntent(
        title: String,
        message: String,
        intent: Intent,
        channelId: String,
        notificationManager: NotificationManager
    ) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableLights(true)
                enableVibration(true)

                // Different vibration patterns based on notification type
                if (title.contains("Work") || title.contains("Service Provider") ||
                    title.contains("Booking") || title.contains("Confirmation")) {
                    // Booking/Service related - stronger vibration + sound
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                } else {
                    // Chat messages - vibration only
                    vibrationPattern = longArrayOf(0, 200)
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification with appropriate pattern
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // Set notification pattern based on type
        if (title.contains("Work") || title.contains("Service Provider") ||
            title.contains("Booking") || title.contains("Confirmation")) {
            notificationBuilder
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            notificationBuilder
                .setVibrate(longArrayOf(0, 200))
        }

        // Show notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}