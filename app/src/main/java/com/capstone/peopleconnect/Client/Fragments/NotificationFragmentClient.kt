// PART OF NOTIFICATION
package com.capstone.peopleconnect.Client.Fragments

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
import androidx.appcompat.app.AlertDialog
import com.capstone.peopleconnect.Classes.BookingProgress
import com.capstone.peopleconnect.Helper.DatabaseHelper


class NotificationFragmentClient : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_notification_client, container, false)

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

                // Get all notifications first
                val allNotifications = snapshot.children.mapNotNull { notificationSnapshot ->
                    val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                    if (notification != null) {
                        notification.isRead = notificationSnapshot.child("isRead").getValue(Boolean::class.java) ?: false
                        notification.id = notificationSnapshot.key ?: ""
                        notification
                    } else null
                }

                // For notifications with bookingId, check booking status
                allNotifications.forEach { notification ->
                    if (notification.bookingId != null) {
                        FirebaseDatabase.getInstance().getReference("bookings")
                            .child(notification.bookingId)
                            .child("bookingStatus")
                            .get()
                            .addOnSuccessListener { statusSnapshot ->
                                val status = statusSnapshot.getValue(String::class.java)
                                // Only add notification if booking is not completed
                                if (status != "Completed") {
                                    notifications.add(notification)
                                    // Update UI
                                    updateNotificationsUI(notifications)
                                } else {
                                    // Remove notification if booking is completed
                                    notificationsRef.child(notification.id).removeValue()
                                    // Also remove any call notifications between these users
                                    removeCallNotifications(notification.senderId)
                                }
                            }
                    } else {
                        // Add non-booking related notifications directly
                        notifications.add(notification)
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationFragment", "Error loading notifications: ${error.message}")
            }
        }

        // Ensure we're using the correct reference path
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            notificationsRef = database.reference
                .child("notifications")
                .child(it.uid)
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
                    Log.e("NotificationFragment", "Error removing call notifications", error.toException())
                }
            })
    }

    private fun updateNotificationsUI(notifications: List<NotificationModel>) {
        // Sort notifications by timestamp (newest first)
        val sortedNotifications = notifications.sortedByDescending { it.timestamp }

        if (sortedNotifications.isEmpty()) {
            view?.findViewById<TextView>(R.id.no_notifications_text_client)?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            view?.findViewById<TextView>(R.id.no_notifications_text_client)?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            notificationAdapter.updateNotifications(sortedNotifications)
        }
    }

    // Update the handleNotificationClick function
    private fun handleNotificationClick(notification: NotificationModel) {
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
                // Handle booking notification click
                notification.bookingId?.let { bookingId ->
                    // Replace the current fragment with BookingDetailsFragment
                    val bookingDetailsFragment = BookingDetailsFragment.newInstance(bookingId, true)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, bookingDetailsFragment)
                        .addToBackStack(null)
                        .commit()
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
                                // Navigate to OngoingFragment first
                                val ongoingFragment = OngoingFragmentClient.newInstance(
                                    bookingId = bookingId,
                                    providerEmail = booking.providerEmail,
                                    clientEmail = booking.bookByEmail
                                )
                                parentFragmentManager.beginTransaction()
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
        }
    }
    private fun showCompletionConfirmationDialog(bookingId: String, providerEmail: String, clientEmail: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Work Completion")
            .setMessage("Has the work been completed?")
            .setPositiveButton("Yes") { _, _ ->
                // Update booking progress
                val progress = BookingProgress(
                    state = BookingProgress.STATE_COMPLETE,
                    bookingId = bookingId,
                    providerEmail = providerEmail,
                    clientEmail = clientEmail,
                    timestamp = System.currentTimeMillis()
                )
                DatabaseHelper.updateBookingProgress(bookingId, progress).addOnSuccessListener {
                    // Update booking status in main bookings table
                    val bookingRef = FirebaseDatabase.getInstance().getReference("bookings/$bookingId")
                    bookingRef.child("bookingStatus").setValue("Completed").addOnSuccessListener {
                        // Navigate to rating screen
                        val rateFragment = RateFragmentClient.newInstance(
                            bookingId = bookingId,
                            providerEmail = providerEmail,
                            clientEmail = clientEmail
                        )
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, rateFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            .setNegativeButton("No") { _, _ ->
                // Update progress back to working state
                val progress = BookingProgress(
                    state = BookingProgress.STATE_WORKING,
                    bookingId = bookingId,
                    providerEmail = providerEmail,
                    clientEmail = clientEmail,
                    timestamp = System.currentTimeMillis()
                )
                DatabaseHelper.updateBookingProgress(bookingId, progress).addOnSuccessListener {
                    // Remove the current completion notification
                    notificationsRef.orderByChild("bookingId")
                        .equalTo(bookingId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                snapshot.children.forEach { notificationSnapshot ->
                                    val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                                    if (notification?.progressState == "awaiting_confirmation") {
                                        notificationSnapshot.ref.removeValue()
                                    }
                                }

                                // Send notification to service provider
                                FirebaseDatabase.getInstance().getReference("users")
                                    .orderByChild("email")
                                    .equalTo(providerEmail)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(providerSnapshot: DataSnapshot) {
                                            val providerId = providerSnapshot.children.firstOrNull()?.key
                                            if (providerId != null) {
                                                // Get client name
                                                FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                                                    FirebaseDatabase.getInstance().getReference("users")
                                                        .child(currentUser.uid)
                                                        .child("name")
                                                        .get()
                                                        .addOnSuccessListener { clientNameSnapshot ->
                                                            val clientName = clientNameSnapshot.getValue(String::class.java) ?: "Client"

                                                            // Create notification for service provider
                                                            val providerNotification = NotificationModel(
                                                                id = FirebaseDatabase.getInstance().reference.push().key ?: return@addOnSuccessListener,
                                                                title = "Work Completion Declined",
                                                                description = "$clientName has declined the work completion. Please double-check your work.",
                                                                type = "ongoing",
                                                                progressState = "work_declined",
                                                                senderId = currentUser.uid,
                                                                senderName = clientName,
                                                                timestamp = System.currentTimeMillis(),
                                                                bookingId = bookingId,
                                                                bookingStatus = "Ongoing"
                                                            )

                                                            // Save notification for service provider
                                                            FirebaseDatabase.getInstance()
                                                                .getReference("notifications")
                                                                .child(providerId)
                                                                .child(providerNotification.id)
                                                                .setValue(providerNotification)
                                                        }
                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e("NotificationFragment", "Error finding provider", error.toException())
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("NotificationFragment", "Error removing notification", error.toException())
                            }
                        })
                }
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the Firebase listener
        notificationListener?.let {
            notificationsRef.removeEventListener(it)
        }
    }
}
