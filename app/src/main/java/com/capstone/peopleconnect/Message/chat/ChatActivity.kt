package com.capstone.peopleconnect.Message.chat

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
//import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ConnectionData
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import io.getstream.chat.android.ui.feature.messages.MessageListFragment
import io.getstream.result.Result
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.chat.android.ui.feature.messages.list.MessageListView


class ChatActivity : AppCompatActivity() {

    private lateinit var chatLoadingSpinner: ProgressBar
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var chatClient: ChatClient
    private var currentUserId: String = ""
    private lateinit var token: String
    private var selectedUserId: String = ""
    private var selectedUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
        /* Notification-related code commented out
        // Add this to test notifications when the activity starts
        testPushNotification()

        // Check if this was opened from a call notification
        val isFromCallNotification = intent.getBooleanExtra("isFromCallNotification", false)
        if (isFromCallNotification) {
            val channelId = intent.getStringExtra("channelId")
            val callId = intent.getStringExtra("callId")
            if (channelId != null && callId != null) {
                handleCallNotification(channelId, callId)
            }
        }
        */

        Log.d("ChatActivity", "onCreate called")

        chatLoadingSpinner = findViewById(R.id.chat_loading_spinner)

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        currentUserId = currentUser?.uid ?: ""

        // Initialize ChatClient
        chatClient = ChatClient.instance()

        // Retrieve the selected user's ID from the intent extras
        selectedUserId = intent.getStringExtra("userId") ?: ""
        selectedUserName = intent.getStringExtra("name").orEmpty()

        if (currentUserId.isEmpty() || selectedUserId.isEmpty() || selectedUserName.isEmpty()) {
            Log.e("ChatActivity", """
                Missing required data:
                - currentUserId: $currentUserId
                - selectedUserId: $selectedUserId
                - selectedUserName: $selectedUserName
            """.trimIndent())
            Toast.makeText(this, "Unable to start chat: Missing user information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Request the token from backend using the current user ID
        requestTokenFromBackend(currentUserId)

        // Example ImageButton to start a video call
        val startCallButton: ImageButton = findViewById(R.id.start_call_button)
        startCallButton.setOnClickListener {
            // Show the loading spinner
            chatLoadingSpinner.visibility = View.VISIBLE

            // Change button color to gray
            startCallButton.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))

            // Introduce a delay
            Handler(Looper.getMainLooper()).postDelayed({
                // Reset button color
                startCallButton.clearColorFilter()

                // Start the video call
                startVideoCall()
            }, 1000) // 1000 milliseconds = 1 second delay
        }
    }


// Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
//    private fun handleCallNotification(channelId: String, callId: String) {
//        loadMessageListFragment(channelId)
//
//        // Show a toast to inform the user they can join the call
//        Toast.makeText(
//            this,
//            "Click on the call message to join the video call",
//            Toast.LENGTH_LONG
//        ).show()
//    }

    //  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
    // Add this function to test push notifications
//    private fun testPushNotification() {
//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val token = task.result
//                Log.d("PushTest", "FCM Token: $token")
//            } else {
//                Log.e("PushTest", "Failed to get FCM token", task.exception)
//            }
//        }
//    }



    private fun startVideoCall() {
        chatLoadingSpinner.visibility = View.VISIBLE
        Log.d("VideoCall", "Start video call button clicked")

        val callId = "$currentUserId$selectedUserId"
        val callLink = "https://getstream.io/video/demos/join/$callId?id=$callId"

        // Ensure the channel ID is consistent
        val sortedUserIds = listOf(currentUserId, selectedUserId).sorted()
        val sanitizedUserIds = sortedUserIds.joinToString("_").replace(Regex("[^a-z0-9_-]"), "").lowercase()
        val channelPrefix = "messaging"
        val channelId = "$channelPrefix:$sanitizedUserIds"

        // Create message with just the call link as text
        val message = Message(
            text = callLink,  // Just send the link in chat
            type = "regular",
            user = User(id = currentUserId)
        )

        val channel = chatClient.channel(channelPrefix, sanitizedUserIds)
        channel.sendMessage(message).enqueue { result ->
            chatLoadingSpinner.visibility = View.GONE

            if (result.isSuccess) {
                //  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
                // Send notification with the call icon
//                sendNotificationData(
//                    userId = selectedUserId,
//                    title = "📞 Video Call Invitation",  // Show call icon in notification
//                    message = "Tap to join the video call",
//                    type = "call",
//                    channelId = channelId,
//                    callLink = callLink
//                )
                Log.d("VideoCall", "Message and notification sent successfully")
            } else {
                Log.e("VideoCall", "Error sending message: ${result.errorOrNull()?.message}")
            }
        }
    }


    //  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

//    // Update the sendNotificationData function to include callLink
//    private fun sendNotificationData(
//        userId: String,
//        title: String,
//        message: String,
//        type: String,
//        channelId: String? = null,
//        callLink: String? = null
//    ) {
//        val database = FirebaseDatabase.getInstance()
//        val notificationsRef = database.reference.child("notifications").child(userId)
//
//        val notification = NotificationModel(
//            id = database.reference.push().key ?: return,
//            title = title,
//            description = message,
//            type = type,
//            senderId = currentUserId,
//            senderName = currentUser?.displayName ?: "Unknown",
//            timestamp = System.currentTimeMillis(),
//            isRead = false,
//            channelId = channelId,
//            callLink = callLink  // Add this field to your NotificationModel
//        )
//
//        notificationsRef.child(notification.id).setValue(notification)
//            .addOnSuccessListener {
//                Log.d("Notification", "Notification data saved successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.e("Notification", "Error saving notification data", e)
//            }
//    }




    private fun requestTokenFromBackend(userId: String) {
        chatLoadingSpinner.visibility = View.VISIBLE // Show spinner at the start

        val url = "https://peopleconnect-chat-backend.vercel.app/"

        val requestBody = JSONObject().apply {
            put("userId", userId)
        }
        Log.d("TokenRequest", "Request payload: $requestBody")

        val requestQueue = Volley.newRequestQueue(this)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            Response.Listener { response ->
                chatLoadingSpinner.visibility = View.GONE // Hide spinner on success
                token = response.optString("token")
                if (token.isNotEmpty()) {
                    Log.d("TokenRequest", "Token received successfully: $token")
                    connectCurrentUserToStreamChat()
                } else {
                    Log.e("TokenRequest", "Token is empty in the response.")
                    Toast.makeText(this, "Failed to retrieve token. Please try again.", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                chatLoadingSpinner.visibility = View.GONE // Hide spinner on error
                val networkResponse = error.networkResponse
                val statusCode = networkResponse?.statusCode ?: "Unknown"
                val errorData = networkResponse?.data?.let { String(it) } ?: "No response data"
                Log.e("TokenRequest", "Error fetching token: HTTP Status $statusCode. Response: $errorData")
                Toast.makeText(this, "Failed to fetch token. Please try again.", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Content-Type" to "application/json")
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun upsertSelectedUser() {
        chatLoadingSpinner.visibility = View.VISIBLE // Show spinner at the start

        val url = "https://peopleconnect-chat-backend.vercel.app/upsert-user"
        val requestBody = JSONObject().apply {
            put("userId", selectedUserId)
            put("userName", selectedUserName)
        }

        Log.d("UpsertUser", "Requesting upsert for userId: $selectedUserId, userName: $selectedUserName")

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            Response.Listener { response ->
                chatLoadingSpinner.visibility = View.GONE // Hide spinner on success
                Log.d("UpsertUser", "User upserted successfully: $response")
                createOrGetChannel()
            },
            Response.ErrorListener { error ->
                chatLoadingSpinner.visibility = View.GONE // Hide spinner on error
                Log.e("UpsertUser", "Error upserting user: ${error.networkResponse?.statusCode}. Response: ${String(error.networkResponse?.data ?: "No response".toByteArray())}")
                Toast.makeText(this, "Upsert failed; skipping to create channel.", Toast.LENGTH_SHORT).show()
                createOrGetChannel()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }




    private fun connectCurrentUserToStreamChat() {
        if (token.isEmpty()) {
            Log.e("ConnectUser", "Token is empty. Cannot connect user.")
            Toast.makeText(this, "Token is missing, cannot connect to chat.", Toast.LENGTH_SHORT).show()
            return
        }

        chatLoadingSpinner.visibility = View.VISIBLE // Show spinner at the start

        val user = User(
            id = currentUserId,
            name = currentUser?.displayName ?: "Anonymous"
        )

        chatClient.connectUser(user, token).enqueue { result: Result<ConnectionData> ->
            chatLoadingSpinner.visibility = View.GONE // Hide spinner after connection attempt

            if (result.isSuccess) {
                Log.d("ConnectUser", "Current user connected successfully to Stream Chat.")
                upsertSelectedUser()
            } else {
                val errorMessage = result.errorOrNull()?.message ?: "Unknown error"
                Log.e("ConnectUser", "Error connecting user: $errorMessage")
                Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createOrGetChannel() {
        chatLoadingSpinner.visibility = View.VISIBLE // Show spinner at the start

        if (currentUserId.isEmpty() || selectedUserId.isEmpty()) {
            Log.e("createOrGetChannel", "User IDs are missing. Cannot create channel.")
            Toast.makeText(this, "User data missing. Cannot create channel.", Toast.LENGTH_SHORT).show()
            chatLoadingSpinner.visibility = View.GONE // Hide spinner if there's an error
            return
        }

        val sortedUserIds = listOf(currentUserId, selectedUserId).sorted()
        val sanitizedUserIds = sortedUserIds.joinToString("_").replace(Regex("[^a-z0-9_-]"), "").lowercase()
        val channelPrefix = "messaging"
        val channelId = "$channelPrefix:$sanitizedUserIds"

        Log.d("createOrGetChannel", "Generated Channel ID: $channelId")

        val members = listOf(currentUserId, selectedUserId)
        val extraData = mapOf("name" to selectedUserName)

        chatClient.channel(channelPrefix, sanitizedUserIds)
            .create(members, extraData)
            .enqueue { result: Result<Channel> ->
                chatLoadingSpinner.visibility = View.GONE // Hide spinner after channel creation attempt

                if (result.isSuccess) {
                    Log.d("createOrGetChannel", "Channel created or retrieved successfully.")
                    loadMessageListFragment(channelId)
                } else {
                    val errorMessage = result.errorOrNull()?.message ?: "Unknown error"
                    Log.e("createOrGetChannel", "Error creating/retrieving the channel: $errorMessage")
                    Toast.makeText(this, "Failed to create/retrieve channel.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadMessageListFragment(channelId: String) {
        val fragmentContainer = findViewById<View>(R.id.channel_fragment_container)
        fragmentContainer.visibility = View.VISIBLE

        if (supportFragmentManager.findFragmentById(R.id.channel_fragment_container) == null) {
            val fragment = MessageListFragment.newInstance(channelId) {
                showHeader(true)
                MessageListView.MessageClickListener { message ->
                    // Check if this is a video call message
                    if (message.extraData.containsKey("is_video_call") &&
                        message.extraData["is_video_call"] as? Boolean == true) {
                        try {
                            // Get the call link from the message
                            val callLink = message.extraData["call_link"] as? String
                            if (callLink != null) {
                                // Open the call link
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(callLink))
                                startActivity(intent)
                                Toast.makeText(this@ChatActivity, "Joining video call...", Toast.LENGTH_SHORT).show()
                                Log.d("VideoCall", "Opening call link from message: $callLink")
                            } else {
                                Toast.makeText(this@ChatActivity, "Call link not found", Toast.LENGTH_SHORT).show()
                                Log.e("VideoCall", "Call link is null in message")
                            }
                        } catch (e: Exception) {
                            Log.e("VideoCall", "Error opening call link from message", e)
                            Toast.makeText(this@ChatActivity, "Unable to open call link", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.channel_fragment_container, fragment)
                .commit()
            Log.d("MessageListFragment", "Message list fragment loaded with message click listener")
        }
    }
}