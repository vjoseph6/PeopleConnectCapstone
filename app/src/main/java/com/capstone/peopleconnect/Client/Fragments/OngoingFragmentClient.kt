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

class OngoingFragmentClient : Fragment() {
    private lateinit var binding: FragmentOngoingClientBinding
    private var bookingId: String? = null
    private var providerEmail: String? = null
    private var email: String? = null  // Add this property
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

        // Modify the existing click listener for the View Message button
        binding.btnViewMessage.setOnClickListener {
            // Check if the button text is "Well Done"
            if (binding.btnViewMessage.text.toString() == "Well Done") {
                // Do nothing or show a toast if you want to inform the user
                Toast.makeText(requireContext(), "Service completed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Existing chat navigation logic
            providerEmail?.let { email ->
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val providerData = snapshot.children.firstOrNull()
                            val providerId = providerData?.child("userId")?.getValue(String::class.java)
                            val providerName = providerData?.child("name")?.getValue(String::class.java)

                            if (providerId != null && providerName != null) {
                                val intent = Intent(requireContext(), ChatActivity::class.java)
                                intent.putExtra("userId", providerId)
                                intent.putExtra("name", providerName)
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
            }
            BookingProgress.STATE_ARRIVE -> {
                binding.btnViewMessage.text = "View Message"
            }
            BookingProgress.STATE_WORKING -> {
                binding.btnViewMessage.text = "View Message"
            }
            "AWAITING_CLIENT_CONFIRMATION" -> {
                showCompletionConfirmationDialog()
            }
            BookingProgress.STATE_COMPLETE -> {
                binding.btnViewMessage.text = "Well Done"
            }
        }
    }

    private fun showCompletionConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Work Completion")
            .setMessage("Has the work been completed?")
            .setPositiveButton("Yes") { _, _ ->
                bookingId?.let { id ->
                    // Update booking progress
                    val progress = BookingProgress(
                        state = BookingProgress.STATE_COMPLETE,
                        bookingId = id,
                        providerEmail = providerEmail ?: "",
                        clientEmail = email ?: "",
                        timestamp = System.currentTimeMillis()
                    )
                    DatabaseHelper.updateBookingProgress(id, progress).addOnSuccessListener {
                        // Update booking status in main bookings table
                        val bookingRef = FirebaseDatabase.getInstance().getReference("bookings/$id")
                        bookingRef.child("bookingStatus").setValue("Completed").addOnSuccessListener {
                            // Navigate to rating screen with all required parameters
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
                }
            }
            .setNegativeButton("No") { _, _ ->
                bookingId?.let { id ->
                    val progress = BookingProgress(
                        state = BookingProgress.STATE_WORKING,
                        bookingId = id,
                        providerEmail = providerEmail ?: "",
                        clientEmail = email ?: "",
                        timestamp = System.currentTimeMillis()
                    )
                    DatabaseHelper.updateBookingProgress(id, progress)
                }
            }
            .setCancelable(false)
            .show()
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