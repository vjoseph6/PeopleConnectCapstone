package com.capstone.peopleconnect.Message.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ConnectionData
import io.getstream.chat.android.models.User
import io.getstream.chat.android.ui.feature.messages.MessageListFragment
import io.getstream.result.Result
import org.json.JSONObject

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

        // Validate user information before proceeding
        if (currentUserId.isEmpty() || selectedUserId.isEmpty() || selectedUserName.isEmpty()) {
            Toast.makeText(this, "User data is missing", Toast.LENGTH_SHORT).show()
            Log.e("ChatActivity", "Missing required user data")
            finish() // Exit the activity if data is missing
            return
        }

        // Request the token from backend using the current user ID
        requestTokenFromBackend(currentUserId)
    }

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
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.channel_fragment_container, fragment)
                .commit()
            Log.d("MessageListFragment", "Message list loaded successfully.")
        }
    }
}