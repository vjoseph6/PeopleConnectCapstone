package com.capstone.peopleconnect.Message

import android.app.Application
// Notification-related imports (uncommented out)
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.capstone.peopleconnect.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.Device
import io.getstream.chat.android.models.PushProvider
import io.getstream.chat.android.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Add this line after Firebase initialization
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)

        // Notification channel creation (commented out)

        // Create notification channel for Android O and above
        createNotificationChannel()


        // Enable background sync and user presence
        val backgroundSyncEnabled = true
        val userPresence = true

        // Initialize the State plugin configuration
        val statePluginFactory = StreamStatePluginFactory(
            StatePluginConfig(
                backgroundSyncEnabled = backgroundSyncEnabled,
                userPresence = userPresence
            ),
            this
        )

        // Initialize the Offline plugin without additional config
        val offlinePluginFactory = StreamOfflinePluginFactory(
            appContext = this
        )

        // Initialize ChatClient with the plugins
        val client = ChatClient.Builder(getString(R.string.api_key), this)
            .logLevel(ChatLogLevel.ALL)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .withPlugins(statePluginFactory, offlinePluginFactory)
            .build()

        // FCM token registration (uncommented out)
        // Add FCM token registration
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result

            // Get current user
            FirebaseAuth.getInstance().currentUser?.let { user ->
                // Create a Device object with the FCM token
                val device = Device(
                    token = token,
                    pushProvider = PushProvider.FIREBASE,
                    providerName = "firebase"
                )

                // Register token with Stream Chat
                client.addDevice(device)

                // Also store in Firebase
                val database = FirebaseDatabase.getInstance()
                val userRef = database.getReference("users").child(user.uid)
                userRef.child("fcmToken").setValue(token)

                Log.d("FCM", "Token registered: $token")
            }
        }

    }

    // Notification channel creation function (uncommented out)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "chat_messages"
            val channelName = "Chat Messages"
            val channelDescription = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }

            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("Notifications", "Chat notification channel created")
        }
    }
}