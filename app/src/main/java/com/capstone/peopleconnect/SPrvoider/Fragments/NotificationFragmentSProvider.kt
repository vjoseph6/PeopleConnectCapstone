package com.capstone.peopleconnect.SPrvoider.Fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.BookingDetailsFragment
import com.capstone.peopleconnect.Message.chat.ChatActivity
import com.capstone.peopleconnect.Notifications.adapter.NotificationAdapter
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationFragmentSProvider : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var notificationsRef: DatabaseReference
    private var notificationListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_s_provider, container, false)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            notificationsRef = database.reference
                .child("notifications")
                .child(currentUser.uid)
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.notification_recycler_view)

        setupRecyclerView()
        setupBackButton(view)
        loadNotifications()

        return view
    }


    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(requireContext()) { notification ->
            handleNotificationClick(notification)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupBackButton(view: View) {
        view.findViewById<ImageView>(R.id.back_button_notify).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadNotifications() {
        notificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<NotificationModel>()

                for (notificationSnapshot in snapshot.children) {
                    try {
                        // Debug log for raw data
                        Log.d("NotificationDebug", "Raw notification data: ${notificationSnapshot.value}")

                        // Get bookingId first and log it
                        val bookingId = notificationSnapshot.child("bookingId").getValue(String::class.java)
                        Log.d("NotificationDebug", "Extracted bookingId: $bookingId")

                        // Get all fields explicitly from snapshot
                        val notification = NotificationModel(
                            id = notificationSnapshot.key ?: "",
                            title = notificationSnapshot.child("title").getValue(String::class.java) ?: "",
                            description = notificationSnapshot.child("description").getValue(String::class.java) ?: "",
                            type = notificationSnapshot.child("type").getValue(String::class.java) ?: "",
                            senderId = notificationSnapshot.child("senderId").getValue(String::class.java) ?: "",
                            senderName = notificationSnapshot.child("senderName").getValue(String::class.java) ?: "",
                            timestamp = notificationSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                            isRead = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false,
                            bookingId = bookingId,
                            bookingStatus = notificationSnapshot.child("bookingStatus").getValue(String::class.java),
                            bookingDate = notificationSnapshot.child("bookingDate").getValue(String::class.java),
                            bookingTime = notificationSnapshot.child("bookingTime").getValue(String::class.java)
                        )

                        Log.d("NotificationDebug", "Created notification object with bookingId: ${notification.bookingId}")
                        notifications.add(notification)
                    } catch (e: Exception) {
                        Log.e("NotificationDebug", "Error parsing notification: ${e.message}", e)
                        continue
                    }
                }

                Log.d("NotificationDebug", "Total notifications loaded: ${notifications.size}")
                // Sort notifications by timestamp (newest first)
                notifications.sortByDescending { it.timestamp }

                // Update UI
                if (notifications.isEmpty()) {
                    Log.d("NotificationDebug", "No notifications to display")
                    view?.findViewById<TextView>(R.id.no_notifications_text)?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    view?.findViewById<TextView>(R.id.no_notifications_text)?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    notificationAdapter.updateNotifications(notifications)
                    Log.d("NotificationDebug", "Updated adapter with ${notifications.size} notifications")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationDebug", "Error loading notifications: ${error.message}")
            }
        }

        notificationsRef.addValueEventListener(notificationListener!!)
    }

    // Update the handleNotificationClick function
    private fun handleNotificationClick(notification: NotificationModel) {
        try {
            // Mark notification as read
            notification.id.let { notificationId ->
                notificationsRef.child(notificationId).child("isRead").setValue(true)
            }

        when (notification.type) {
            "chat" -> {
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("userId", notification.senderId)
                    putExtra("name", notification.senderName)
                    notification.channelId?.let { putExtra("channelId", it) }
                }
                startActivity(intent)
            }
            "call" -> {
                try {
                    // Use the callLink from the notification
                    notification.callLink?.let { callLink ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(callLink))
                        startActivity(intent)
                        Toast.makeText(context, "Joining video call...", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(context, "Call link not found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("NotificationFragment", "Error opening call link", e)
                    Toast.makeText(context, "Unable to join call", Toast.LENGTH_SHORT).show()
                }
            }
            "booking" -> {
                Log.d("NotificationClick", "Booking ID: ${notification.bookingId}")
                notification.bookingId?.let { bookingId ->
                    // Create and navigate to BookingDetailsFragment
                    val bookingDetailsFragment = BookingDetailsFragment.newInstance(bookingId, false)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, bookingDetailsFragment)
                        .addToBackStack(null)
                        .commit()
                } ?: run {
                    Log.e("NotificationClick", "Booking ID is null")
                    Toast.makeText(context, "Booking details not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        } catch (e: Exception) {
            Log.e("NotificationClick", "Error handling notification click", e)
            Toast.makeText(context, "Error opening booking details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the Firebase listener
        notificationListener?.let {
            notificationsRef.removeEventListener(it)
        }
    }
}