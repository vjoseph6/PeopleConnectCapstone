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
import com.capstone.peopleconnect.Classes.Bookings
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
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.Client.Fragments.PostDetailsFragment


class NotificationFragmentSProvider : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var notificationsRef: DatabaseReference
    private var notificationListener: ValueEventListener? = null

    companion object {
        private const val TAG = "NotificationFragmentSProvider"
    }

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(user.uid)

            notificationListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<NotificationModel>()

                    // Process all notifications
                    val allNotifications = snapshot.children.mapNotNull { notificationSnapshot ->
                        notificationSnapshot.getValue(NotificationModel::class.java)?.apply {
                            id = notificationSnapshot.key ?: ""
                            isRead = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false
                        }
                    }

                    // Handle notifications
                    allNotifications.forEach { notification ->
                        if (notification.bookingId != null) {
                            // Check booking status for notifications with bookingId
                            FirebaseDatabase.getInstance().getReference("bookings")
                                .child(notification.bookingId!!)
                                .child("bookingStatus")
                                .get()
                                .addOnSuccessListener { statusSnapshot ->
                                    val status = statusSnapshot.getValue(String::class.java)
                                    if (status != "Completed") {
                                        // Add if booking is not completed
                                        notifications.add(notification)
                                    } else {
                                        // Remove completed booking notifications
                                        notificationsRef.child(notification.id).removeValue()
                                        removeCallNotifications(notification.senderId)
                                    }
                                    // Update UI after checking status
                                    updateNotificationsUI(notifications.sortedByDescending { it.timestamp })
                                }
                        } else if (notification.type in listOf(
                                "booking", "call", "chat", "ongoing",
                                "post_request", "post_status", "application_status",
                                "post_application"
                            )) {
                            // Add non-booking notifications of specific types
                            notifications.add(notification)
                        }
                    }

                    // Update UI for non-booking notifications
                    updateNotificationsUI(notifications.sortedByDescending { it.timestamp })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationDebug", "Error loading notifications: ${error.message}")
                }
            }

            notificationsRef.addValueEventListener(notificationListener!!)
        }
    }

    private fun removeCallNotifications(userId: String) {
        notificationsRef.orderByChild("senderId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { notificationSnapshot ->
                        val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                        if (notification?.type == "call") {
                            notificationSnapshot.ref.removeValue()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationDebug", "Error removing call notifications", error.toException())
                }
            })
    }

    private fun updateNotificationsUI(notifications: List<NotificationModel>) {
        // Sort notifications by timestamp (newest first)
        val sortedNotifications = notifications.sortedByDescending { it.timestamp }

        if (sortedNotifications.isEmpty()) {
            view?.findViewById<TextView>(R.id.no_notifications_text)?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            view?.findViewById<TextView>(R.id.no_notifications_text)?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            notificationAdapter.updateNotifications(sortedNotifications)
        }
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
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

                "ongoing" -> {
                    notification.bookingId?.let { bookingId ->
                        // Get the booking details first to get correct emails
                        FirebaseDatabase.getInstance().getReference("bookings")
                            .child(bookingId)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val booking = snapshot.getValue(Bookings::class.java)
                                if (booking != null) {
                                    val ongoingFragment = OngoingFragmentSProvider.newInstance(
                                        bookingId = bookingId,
                                        clientEmail = booking.bookByEmail,  // Use actual client email
                                        providerEmail = booking.providerEmail  // Use actual provider email
                                    )
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .replace(R.id.frame_layout, ongoingFragment)
                                        .addToBackStack(null)
                                        .commit()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error loading booking details", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                "post_request" -> {
                    // Navigate to post details when service provider clicks the notification
                    notification.postId?.let { postId ->
                        FirebaseDatabase.getInstance().reference
                            .child("posts")
                            .child(postId)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val post = snapshot.getValue(Post::class.java)
                                if (post != null) {
                                    val postDetailsFragment = PostDetailsFragment.newInstance(
                                        post = post,
                                        isFromProvider = true,
                                        providerEmail = FirebaseAuth.getInstance().currentUser?.email
                                    )
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .replace(R.id.frame_layout, postDetailsFragment)
                                        .addToBackStack(null)
                                        .commit()
                                }
                            }
                    }
                }

                "application_status" -> {
                    // Navigate to post details when provider clicks accept/reject notification
                    notification.postId?.let { postId ->
                        FirebaseDatabase.getInstance().reference
                            .child("posts")
                            .child(postId)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val post = snapshot.getValue(Post::class.java)
                                if (post != null) {
                                    val postDetailsFragment = PostDetailsFragment.newInstance(
                                        post = post,
                                        isFromProvider = true,
                                        providerEmail = FirebaseAuth.getInstance().currentUser?.email
                                    )
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .replace(R.id.frame_layout, postDetailsFragment)
                                        .addToBackStack(null)
                                        .commit()
                                }
                            }
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