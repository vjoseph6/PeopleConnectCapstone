package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.databinding.FragmentOngoingClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.capstone.peopleconnect.Classes.BookingProgress
import com.capstone.peopleconnect.Helper.DatabaseHelper
import com.capstone.peopleconnect.Helper.NetworkHelper
import com.capstone.peopleconnect.Message.chat.ChatActivity
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class OngoingFragmentClient : Fragment() {
    private lateinit var binding: FragmentOngoingClientBinding
    private var bookingId: String? = null
    private var providerEmail: String? = null
    private var email: String? = null  // Add this property
    private lateinit var notificationsRef: DatabaseReference
    private var currentState = BookingProgress.STATE_PENDING
    private var progressListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString(ARG_BOOKING_ID)
            providerEmail = it.getString(ARG_PROVIDER_EMAIL)
            email = it.getString(ARG_CLIENT_EMAIL)  // Add this line
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOngoingClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add network monitoring
        NetworkHelper.startNetworkMonitoring(requireContext())
        NetworkHelper.isNetworkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            if (!isAvailable) {
                binding.btnViewMessage.text = "No Internet Connection"
            } else {
                validateStateTransition(currentState)
            }
        }

        // Initialize notificationsRef
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            notificationsRef = FirebaseDatabase.getInstance().reference
                .child("notifications")
                .child(currentUser.uid)
        }

        // Add long click listener to status message
        binding.statusMessage.setOnLongClickListener {
            if (currentState == "AWAITING_CLIENT_CONFIRMATION") {
                showCompletionConfirmationDialog()
                true
            } else {
                false
            }
        }

        // Setup back button
        binding.btnBackClient.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load provider details
        providerEmail?.let { email ->
            loadProviderDetails(email)
        }

        // Setup progress listener
        bookingId?.let { id ->
            setupProgressListener(id)
            // Add booking status check
            checkBookingStatus()
        }

        binding.btnViewMessage.setOnClickListener {
            // Get provider's user ID using their email
            providerEmail?.let { email ->
                FirebaseDatabase.getInstance().getReference("users")
                    .orderByChild("email")
                    .equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val providerData = snapshot.children.firstOrNull()
                            val providerId = providerData?.child("userId")?.getValue(String::class.java)
                            val providerName = providerData?.child("name")?.getValue(String::class.java)

                            if (providerId != null && providerName != null) {
                                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                                    putExtra("userId", providerId)
                                    putExtra("name", providerName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("OngoingFragment", "Error fetching provider details", error.toException())
                        }
                    })
            }
        }
    }

    private fun checkBookingStatus() {
        bookingId?.let { id ->
            val bookingRef = FirebaseDatabase.getInstance().getReference("bookings/$id")
            bookingRef.child("bookingStatus").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status == "Complete") {
                        // Check if the fragment is still attached to avoid crashes
                        if (!isAdded) return

                        // Navigate to rating if not already rated
                        val rateFragment = RateFragmentClient.newInstance(
                            bookingId = id,
                            providerEmail = providerEmail ?: "",
                            clientEmail = email ?: ""
                        )
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, rateFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OngoingFragmentClient", "Error checking booking status", error.toException())
                }
            })
        }
    }

    private fun setupProgressListener(bookingId: String) {
        // Remove existing listener if any
        progressListener?.let { listener ->
            DatabaseHelper.getBookingProgressReference(bookingId).removeEventListener(listener)
        }

        // Create new listener
        progressListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return  // Check if fragment is still attached

                try {
                    val progress = snapshot.getValue(BookingProgress::class.java)
                    progress?.let {
                        if (it.state != currentState) {
                            currentState = it.state
                            updateUI(currentState)
                            // Add this explicit check for awaiting confirmation
                            if (currentState == BookingProgress.STATE_AWAITING_CONFIRMATION) {
                                showCompletionConfirmationDialog()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OngoingFragmentClient", "Error processing progress update", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Log.e("OngoingFragmentClient", "Error loading progress", error.toException())
            }
        }

        // Attach new listener
        DatabaseHelper.getBookingProgressReference(bookingId)
            .addValueEventListener(progressListener!!)
    }

    private fun validateStateTransition(state: String) {
        when (state) {
            BookingProgress.STATE_PENDING -> {
                binding.btnViewMessage.text = "View Message"
                binding.statusMessage.text = "Waiting for service provider..."
                binding.statusMessage.visibility = View.VISIBLE
            }
            BookingProgress.STATE_ARRIVE -> {
                binding.btnViewMessage.text = "View Message"
                binding.statusMessage.text = "Service provider has arrived at your location"
                binding.statusMessage.visibility = View.VISIBLE
            }
            BookingProgress.STATE_WORKING -> {
                binding.btnViewMessage.text = "View Message"
                binding.statusMessage.text = "Service provider is currently working"
                binding.statusMessage.visibility = View.VISIBLE
            }
            "AWAITING_CLIENT_CONFIRMATION" -> {
                binding.statusMessage.text = "Please confirm if the work is completed"
                binding.statusMessage.visibility = View.VISIBLE
            }
            BookingProgress.STATE_COMPLETE -> {
                binding.btnViewMessage.text = "Well Done"
                binding.statusMessage.text = "Service completed"
                binding.statusMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun showCompletionConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Work Completion")
            .setMessage("Has the work been completed?")
            .setPositiveButton("Yes") { _, _ ->
                bookingId?.let { id ->
                    handleWorkCompletion(id)
                }
            }
            .setNegativeButton("No") { _, _ ->
                bookingId?.let { id ->
                    handleWorkDeclined(id)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun handleWorkCompletion(bookingId: String) {
        // First update the booking status
        val bookingRef = FirebaseDatabase.getInstance().getReference("bookings/$bookingId")
        bookingRef.child("bookingStatus").setValue("Completed")
            .addOnSuccessListener {
                // After booking status is updated, update the progress
                val progress = BookingProgress(
                    state = BookingProgress.STATE_COMPLETE,
                    bookingId = bookingId,
                    providerEmail = providerEmail ?: "",
                    clientEmail = email ?: "",
                    timestamp = System.currentTimeMillis()
                )

                DatabaseHelper.updateBookingProgress(bookingId, progress)
                    .addOnSuccessListener {
                        // Remove notifications only after both updates are successful
                        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                            removeOngoingNotifications(currentUser.uid, bookingId) {
                                // Navigate to rating screen only after all updates are complete
                                navigateToRating(bookingId)
                            }
                        }
                    }
                    .addOnFailureListener { error ->
                        handleError("Failed to update progress", error)
                    }
            }
            .addOnFailureListener { error ->
                handleError("Failed to update booking status", error)
            }
    }

    private fun handleWorkDeclined(bookingId: String) {
        val progress = BookingProgress(
            state = BookingProgress.STATE_WORKING,
            bookingId = bookingId,
            providerEmail = providerEmail ?: "",
            clientEmail = email ?: "",
            timestamp = System.currentTimeMillis()
        )

        DatabaseHelper.updateBookingProgress(bookingId, progress)
            .addOnSuccessListener {
                FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                    removeOngoingNotifications(currentUser.uid, bookingId) {
                        sendWorkDeclinedNotifications(bookingId, currentUser.uid)
                    }
                }
            }
            .addOnFailureListener { error ->
                handleError("Failed to decline work", error)
            }
    }

    private fun removeOngoingNotifications(userId: String, bookingId: String, onComplete: () -> Unit) {
        val notificationsRef = FirebaseDatabase.getInstance().reference
            .child("notifications")
            .child(userId)

        notificationsRef.orderByChild("bookingId")
            .equalTo(bookingId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var removedCount = 0
                    val totalToRemove = snapshot.children.count {
                        it.getValue(NotificationModel::class.java)?.type == "ongoing"
                    }

                    if (totalToRemove == 0) {
                        onComplete()
                        return
                    }

                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                        if (notification?.type == "ongoing") {
                            notificationSnapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    removedCount++
                                    if (removedCount == totalToRemove) {
                                        onComplete()
                                    }
                                }
                                .addOnFailureListener { error ->
                                    Log.e("OngoingFragment", "Error removing notification", error)
                                    removedCount++
                                    if (removedCount == totalToRemove) {
                                        onComplete()
                                    }
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OngoingFragment", "Error removing notifications", error.toException())
                    onComplete()
                }
            })
    }

    private fun sendWorkDeclinedNotifications(bookingId: String, clientUid: String) {
        providerEmail?.let { email ->
            FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val providerId = snapshot.children.firstOrNull()?.key ?: return
                        createAndSendNotifications(bookingId, clientUid, providerId)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        handleError("Error finding provider", error.toException())
                    }
                })
        }
    }

    private fun createAndSendNotifications(bookingId: String, clientUid: String, providerId: String) {
        // Create Work Started notification for client
        val workStartedNotification = NotificationModel(
            id = FirebaseDatabase.getInstance().reference.push().key ?: return,
            title = "Work Started",
            description = "Service provider has started working",
            type = "ongoing",
            progressState = "working",
            senderId = providerId,
            senderName = "Service Provider",
            timestamp = System.currentTimeMillis(),
            bookingId = bookingId,
            bookingStatus = "Ongoing"
        )

        // Create decline notification for provider
        val declineNotification = NotificationModel(
            id = FirebaseDatabase.getInstance().reference.push().key ?: return,
            title = "Work Completion Declined",
            description = "Client has declined the work completion. Please double-check your work.",
            type = "ongoing",
            progressState = "work_declined",
            senderId = clientUid,
            senderName = "Client",
            timestamp = System.currentTimeMillis(),
            bookingId = bookingId,
            bookingStatus = "Ongoing"
        )

        // Save both notifications
        val notificationsRef = FirebaseDatabase.getInstance().reference.child("notifications")
        notificationsRef.child(clientUid).child(workStartedNotification.id).setValue(workStartedNotification)
        notificationsRef.child(providerId).child(declineNotification.id).setValue(declineNotification)
    }

    private fun navigateToRating(bookingId: String) {
        if (!isAdded) return  // Check if fragment is still attached

        val rateFragment = RateFragmentClient.newInstance(
            bookingId = bookingId,
            providerEmail = providerEmail ?: "",
            clientEmail = email ?: ""
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, rateFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleError(message: String, error: Exception) {
        Log.e("OngoingFragment", "$message: ${error.message}", error)
        if (isAdded) {
            Toast.makeText(context, "$message. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateUI(state: String) {
        val context = requireContext()
        val greenColor = ContextCompat.getColor(context, R.color.green)

        // Get progress bar elements
        val progressLines = listOf(
            binding.progressStatusBar.getChildAt(1) as View,  // First line
            binding.progressStatusBar.getChildAt(3) as View,  // Second line
            binding.progressStatusBar.getChildAt(5) as View   // Third line
        )

        val progressIcons = listOf(
            binding.progressStatusBar.getChildAt(0) as ImageView,  // First icon
            binding.progressStatusBar.getChildAt(2) as ImageView,  // Second icon
            binding.progressStatusBar.getChildAt(4) as ImageView,  // Third icon
            binding.progressStatusBar.getChildAt(6) as ImageView   // Fourth icon
        )

        // Apply transitions based on state
        binding.illustrationImage.alpha = 0f
        binding.illustrationImage.animate().alpha(1f).duration = 300

        when (state) {
            BookingProgress.STATE_PENDING -> {
                binding.illustrationImage.setImageResource(R.drawable.client_location_ongoing)
            }
            BookingProgress.STATE_ARRIVE -> {
                progressLines[0].setBackgroundColor(greenColor)
                progressIcons[1].setColorFilter(greenColor)

                binding.illustrationImage.setImageResource(R.drawable.client_work_ongoing)
            }
            BookingProgress.STATE_WORKING -> {
                progressLines.take(2).forEach { it.setBackgroundColor(greenColor) }
                progressIcons.take(3).forEach { it.setColorFilter(greenColor) }

                binding.illustrationImage.setImageResource(R.drawable.client_almost_work_done)
            }
            BookingProgress.STATE_COMPLETE -> {
                progressLines.forEach { it.setBackgroundColor(greenColor) }
                progressIcons.forEach { it.setColorFilter(greenColor) }

                binding.illustrationImage.setImageResource(R.drawable.client_well_done_ongoing)
            }
        }
    }

    private fun loadProviderDetails(email: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("email")
            .equalTo(email)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    binding.sproviderName.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(binding.profilePicture)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OngoingFragmentClient", "Error loading provider details", error.toException())
            }
        })
    }


    companion object {
        private const val ARG_BOOKING_ID = "booking_id"
        private const val ARG_PROVIDER_EMAIL = "provider_email"
        private const val ARG_CLIENT_EMAIL = "client_email"

        @JvmStatic
        fun newInstance(bookingId: String, providerEmail: String, clientEmail: String) =
            OngoingFragmentClient().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                    putString(ARG_PROVIDER_EMAIL, providerEmail)
                    putString(ARG_CLIENT_EMAIL, clientEmail)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        // Reattach progress listener when fragment becomes active
        bookingId?.let { id ->
            setupProgressListener(id)
        }
    }

    override fun onPause() {
        super.onPause()
        // Remove progress listener when fragment is inactive
        progressListener?.let { listener ->
            bookingId?.let { id ->
                DatabaseHelper.getBookingProgressReference(id).removeEventListener(listener)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingId?.let { id ->
            DatabaseHelper.getBookingProgressReference(id).removeEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}