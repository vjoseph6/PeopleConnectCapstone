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
import com.capstone.peopleconnect.Notifications.model.NotificationModel
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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.TimeoutError
import com.android.volley.NoConnectionError
import java.nio.charset.Charset
import com.android.volley.RetryPolicy


class ChatActivity : AppCompatActivity() {

    private lateinit var chatLoadingSpinner: ProgressBar
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var chatClient: ChatClient
    private var currentUserId: String = ""
    private lateinit var token: String
    private var selectedUserId: String = ""
    private var selectedUserName: String = ""
    private var isActivityActive = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        isActivityActive = true

        // Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

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

    override fun onDestroy() {
        super.onDestroy()
        isActivityActive = false
    }


    // Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
    private fun handleCallNotification(channelId: String, callId: String) {
        loadMessageListFragment(channelId)

        // Show a toast to inform the user they can join the call
        Toast.makeText(
            this,
            "Click on the call message to join the video call",
            Toast.LENGTH_LONG
        ).show()
    }

    //  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.
    // Add this function to test push notifications
    private fun testPushNotification() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("PushTest", "FCM Token: $token")
            } else {
                Log.e("PushTest", "Failed to get FCM token", task.exception)
            }
        }
    }


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
                sendNotificationData(
                    userId = selectedUserId,
                    title = "📞 Video Call Invitation",  // Show call icon in notification
                    message = "Tap to join the video call",
                    type = "call",
                    channelId = channelId,
                    callLink = callLink
                )
                Log.d("VideoCall", "Message and notification sent successfully")
            } else {
                Log.e("VideoCall", "Error sending message: ${result.errorOrNull()?.message}")
            }
        }
    }


    //  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

    // Update the sendNotificationData function to include callLink
    private fun sendNotificationData(
        userId: String,
        title: String,
        message: String,
        type: String,
        channelId: String? = null,
        callLink: String? = null
    ) {
        val database = FirebaseDatabase.getInstance()
        val notificationsRef = database.reference.child("notifications").child(userId)

        val notification = NotificationModel(
            id = database.reference.push().key ?: return,
            title = title,
            description = message,
            type = type,
            senderId = currentUserId,
            senderName = currentUser?.displayName ?: "Unknown",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            channelId = channelId,
            callLink = callLink  // Add this field to your NotificationModel
        )

        notificationsRef.child(notification.id).setValue(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Notification data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error saving notification data", e)
            }
    }




    private fun requestTokenFromBackend(userId: String) {
        chatLoadingSpinner.visibility = View.VISIBLE

        val url = "https://peopleconnect-chat-backend.vercel.app/"

        val requestBody = JSONObject().apply {
            put("userId", userId)
        }
        Log.d("TokenRequest", "Requesting token for userId: $userId")

        // Create a custom retry policy
        val retryPolicy: RetryPolicy = DefaultRetryPolicy(
            30000, // Timeout in milliseconds
            3,     // Max retries
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        val requestQueue = Volley.newRequestQueue(this)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            Response.Listener { response ->
                chatLoadingSpinner.visibility = View.GONE
                try {
                    token = response.optString("token")
                    if (token.isNotEmpty()) {
                        Log.d("TokenRequest", "Token received successfully")
                        connectCurrentUserToStreamChat()
                    } else {
                        handleError("Token is empty in the response")
                    }
                } catch (e: Exception) {
                    handleError("Error processing response: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                chatLoadingSpinner.visibility = View.GONE

                val errorMessage = when (error) {
                    is TimeoutError -> "Request timed out. Please check your internet connection."
                    is NoConnectionError -> "No internet connection available."
                    else -> {
                        val networkResponse = error.networkResponse
                        val statusCode = networkResponse?.statusCode ?: -1
                        val errorData = networkResponse?.data?.let {
                            String(it, Charset.defaultCharset())
                        } ?: "Unknown error"

                        when (statusCode) {
                            500 -> "Server error. Please try again later. (Error 500)"
                            404 -> "Server endpoint not found. (Error 404)"
                            401 -> "Authentication failed. Please log in again."
                            else -> "Error: $errorData (Status: $statusCode)"
                        }
                    }
                }

                handleError(errorMessage)
                Log.e("TokenRequest", "Error details: $errorMessage", error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json"
                )
            }
        }

        // Set the retry policy
        jsonObjectRequest.retryPolicy = retryPolicy

        // Add request to queue
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
        if (!isActivityActive) {
            Log.w("ChatActivity", "Attempting to create channel after activity destruction")
            return
        }

        chatLoadingSpinner.visibility = View.VISIBLE

        if (currentUserId.isEmpty() || selectedUserId.isEmpty()) {
            Log.e("createOrGetChannel", "User IDs are missing. Cannot create channel.")
            Toast.makeText(this, "User data missing. Cannot create channel.", Toast.LENGTH_SHORT).show()
            chatLoadingSpinner.visibility = View.GONE
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
                if (!isActivityActive) {
                    Log.w("ChatActivity", "Channel created but activity is destroyed")
                    return@enqueue
                }

                chatLoadingSpinner.visibility = View.GONE

                if (result.isSuccess) {
                    Log.d("createOrGetChannel", "Channel created or retrieved successfully.")
                    loadMessageListFragment(channelId)
                } else {
                    val errorMessage = result.errorOrNull()?.message ?: "Unknown error"
                    Log.e("createOrGetChannel", "Error creating/retrieving the channel: $errorMessage")
                    runOnUiThread {
                        Toast.makeText(this, "Failed to create/retrieve channel.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun loadMessageListFragment(channelId: String) {
        if (!isActivityActive) {
            Log.w("ChatActivity", "Attempting to load fragment after activity destruction")
            return
        }

        try {
            val fragmentContainer = findViewById<View>(R.id.channel_fragment_container)
            fragmentContainer.visibility = View.VISIBLE

            if (!supportFragmentManager.isDestroyed &&
                supportFragmentManager.findFragmentById(R.id.channel_fragment_container) == null) {

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

                runOnUiThread {
                    try {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.channel_fragment_container, fragment)
                            .commitAllowingStateLoss()
                        Log.d("MessageListFragment", "Message list fragment loaded with message click listener")
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "Error loading message fragment", e)
                        Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error in loadMessageListFragment", e)
            Toast.makeText(this, "Error loading chat interface", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleError(message: String) {
        Log.e("TokenRequest", message)
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            // Optionally add a retry button
            showRetryDialog()
        }
    }

    private fun showRetryDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Connection Error")
            .setMessage("Failed to connect to the server. Would you like to retry?")
            .setPositiveButton("Retry") { _, _ ->
                requestTokenFromBackend(currentUserId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish() // Optional: close the activity if the user doesn't want to retry
            }
            .setCancelable(false)
            .show()
    }
}