package com.capstone.peopleconnect.Client.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Message.ChatUserDiffCallback
import com.capstone.peopleconnect.Message.adapter.UserAdapters
import com.capstone.peopleconnect.Message.chat.ChatActivity
import com.capstone.peopleconnect.Message.model.ChatUser
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class MessageFragmentClient : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapters
    private var chatUser = mutableListOf<ChatUser>()

    private var currentUser: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var loaderMessage: ProgressBar
    private var isRetrying = false
    private val maxRetries = 3
    private var retryCount = 0
    private lateinit var emptyStateText: TextView
    private var chatConnectionsListener: ValueEventListener? = null
    private lateinit var chatConnectionsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth here
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_message_client, container, false)

        // Initialize chat connections reference
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            chatConnectionsRef = FirebaseDatabase.getInstance()
                .getReference("chat_connections")
                .child(currentUserId)
        }


        // Add empty state TextView initialization
        emptyStateText = rootView.findViewById(R.id.emptyStateText)

        loaderMessage = rootView.findViewById(R.id.LoadUserMessages)
        recyclerView = rootView.findViewById(R.id.message_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        chatUser = ArrayList()

        userAdapter = UserAdapters(chatUser) { user ->
            openChatWithUser(user)
        }
        recyclerView.adapter = userAdapter

        fetchUsersFromFirebase()
        setupRealtimeUpdates()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the back button and set an OnClickListener
        val backButton: ImageView = view.findViewById(R.id.back_button_message)
        backButton.setOnClickListener {
            // Navigate back to the HomeFragmentClient
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupRealtimeUpdates() {
        val currentUserId = getCurrentUserId() ?: return

        chatConnectionsRef = FirebaseDatabase.getInstance()
            .getReference("chat_connections")
            .child(currentUserId)

        chatConnectionsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connectedProviderIds = snapshot.children.mapNotNull {
                    if (it.getValue(Boolean::class.java) == true) it.key else null
                }

                if (connectedProviderIds.isEmpty()) {
                    hideLoading()
                    showEmptyState("No active service providers")
                    return
                }

                fetchUserDetails(connectedProviderIds)
            }

            override fun onCancelled(error: DatabaseError) {
                handleError("Real-time update failed", error.toException())
            }
        }

        chatConnectionsRef.addValueEventListener(chatConnectionsListener!!)
    }

    private fun fetchUsersFromFirebase() {
        val currentUserId = getCurrentUserId() ?: run {
            handleError("User not logged in")
            return
        }

        showLoading()
        retryCount = 0
        fetchChatConnections(currentUserId)
    }

    private fun fetchChatConnections(currentUserId: String) {
        // Change to addValueEventListener for real-time updates
        chatConnectionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(connectionsSnapshot: DataSnapshot) {
                val connectedProviderIds = connectionsSnapshot.children.mapNotNull {
                    if (it.getValue(Boolean::class.java) == true) it.key else null
                }

                if (connectedProviderIds.isEmpty()) {
                    hideLoading()
                    showEmptyState("No chat connections found")
                    return
                }

                fetchUserDetails(connectedProviderIds)
            }

            override fun onCancelled(error: DatabaseError) {
                handleError("Failed to load chat connections", error.toException())
            }
        })
    }

    private fun fetchUserDetails(connectedProviderIds: List<String>) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        val currentUserEmail = auth.currentUser?.email

        showLoading()

        bookingsRef.orderByChild("bookByEmail").equalTo(currentUserEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(bookingsSnapshot: DataSnapshot) {
                    // Get all provider emails with active bookings
                    val activeProviderEmails = bookingsSnapshot.children
                        .mapNotNull { booking ->
                            val status = booking.child("bookingStatus").getValue(String::class.java)
                            val providerEmail = booking.child("providerEmail").getValue(String::class.java)
                            if (status == "Pending" || status == "Accepted") providerEmail else null
                        }

                    // Now fetch user details for providers with active bookings
                    usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(usersSnapshot: DataSnapshot) {
                            try {
                                val newChatUserList = mutableListOf<ChatUser>()

                                for (userSnapshot in usersSnapshot.children) {
                                    val userId = userSnapshot.child("userId").getValue(String::class.java) ?: continue
                                    val email = userSnapshot.child("email").getValue(String::class.java) ?: continue

                                    if (userId in connectedProviderIds && email in activeProviderEmails) {
                                        val name = userSnapshot.child("name").getValue(String::class.java) ?: continue
                                        val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                                        val roles = userSnapshot.child("roles")
                                            .getValue(object : GenericTypeIndicator<List<String>>() {}) ?: continue

                                        if ("Service Provider" in roles) {
                                            newChatUserList.add(ChatUser(userId, name, profileImageUrl))
                                        }
                                    }
                                }

                                updateUserList(newChatUserList)
                                hideLoading()

                                if (newChatUserList.isEmpty()) {
                                    showEmptyState("No active service providers")
                                }

                            } catch (e: Exception) {
                                handleError("Error processing user data", e)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            handleError("Failed to load user details", error.toException())
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError("Failed to load bookings", error.toException())
                }
            })
    }

    // Add these utility functions
    private fun showLoading() {
        loaderMessage.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateText.visibility = View.GONE
    }

    private fun hideLoading() {
        loaderMessage.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        emptyStateText.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        emptyStateText.text = message
        emptyStateText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        loaderMessage.visibility = View.GONE
    }

    private fun handleError(message: String, error: Throwable? = null) {
        hideLoading()
        error?.let {
            Log.e("MessageFragment", message, it)
            // Add chat connection recovery logic
            if (message.contains("chat_connections")) {
                val currentUserId = getCurrentUserId()
                if (currentUserId != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("bookings")
                        .orderByChild("bookByEmail")
                        .equalTo(auth.currentUser?.email)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                snapshot.children.forEach { bookingSnapshot ->
                                    val providerEmail = bookingSnapshot.child("providerEmail").getValue(String::class.java)
                                    val status = bookingSnapshot.child("bookingStatus").getValue(String::class.java)

                                    if (providerEmail != null && (status == "Pending" || status == "Accepted")) {
                                        // Get provider's userId and reestablish connection
                                        FirebaseDatabase.getInstance().getReference("users")
                                            .orderByChild("email")
                                            .equalTo(providerEmail)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                                    val providerId = userSnapshot.children.firstOrNull()
                                                        ?.child("userId")?.getValue(String::class.java)
                                                    if (providerId != null) {
                                                        val databaseRef = FirebaseDatabase.getInstance()
                                                            .getReference("chat_connections")
                                                        databaseRef.child(currentUserId).child(providerId).setValue(true)
                                                        databaseRef.child(providerId).child(currentUserId).setValue(true)
                                                    }
                                                }
                                                override fun onCancelled(dbError: DatabaseError) {
                                                    Log.e("MessageFragment", "Failed to get provider ID", dbError.toException())
                                                }
                                            })
                                    }
                                }
                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e("MessageFragment", "Failed to check bookings", databaseError.toException())
                            }
                        })
                }
            }
        }

        if (retryCount < maxRetries && !isRetrying) {
            retryCount++
            isRetrying = true

            Handler(Looper.getMainLooper()).postDelayed({
                isRetrying = false
                fetchUsersFromFirebase()
            }, 2000) // 2 second delay before retry

            Toast.makeText(context, "$message. Retrying...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            showEmptyState("Unable to load messages")
        }
    }

    private fun updateUserList(newList: List<ChatUser>) {
        val diffCallback = ChatUserDiffCallback(chatUser, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        chatUser.clear()
        chatUser.addAll(newList)
        diffResult.dispatchUpdatesTo(userAdapter)
    }

    private fun getCurrentUserId(): String? {
        currentUser = auth.currentUser
        return currentUser?.uid
    }

    private fun openChatWithUser(user: ChatUser) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("userId", user.userId)
        intent.putExtra("name", user.name)
        Log.d("UserMessage", "Opening chat with user ID: ${user.userId}")
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the listener when the view is destroyed
        chatConnectionsListener?.let {
            chatConnectionsRef.removeEventListener(it)
        }
    }
}
