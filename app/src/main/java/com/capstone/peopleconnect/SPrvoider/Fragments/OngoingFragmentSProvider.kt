package com.capstone.peopleconnect.SPrvoider.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Classes.BookingProgress
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Helper.DatabaseHelper
import com.capstone.peopleconnect.Helper.NetworkHelper
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.databinding.FragmentOngoingSProviderBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlin.math.abs


class OngoingFragmentSProvider : Fragment() {
    private lateinit var binding: FragmentOngoingSProviderBinding
    private var bookingId: String? = null
    private var clientEmail: String? = null
    private var providerEmail: String? = null  // Add this property
    private var currentState = BookingProgress.STATE_PENDING
    private var initialX: Float = 0f
    private val SWIPE_THRESHOLD = 200f  // Minimum distance for swipe
    private var isUpdating = false
    private var progressListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString(ARG_BOOKING_ID)
            clientEmail = it.getString(ARG_CLIENT_EMAIL)
            providerEmail = it.getString(ARG_PROVIDER_EMAIL)  // Add this line
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOngoingSProviderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add network monitoring
        NetworkHelper.startNetworkMonitoring(requireContext())
        NetworkHelper.isNetworkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            binding.btnSwipe.isEnabled = isAvailable && currentState != BookingProgress.STATE_COMPLETE
            if (!isAvailable) {
                binding.btnSwipe.text = "No Internet Connection"
            } else {
                validateStateTransition(currentState)
            }
        }

        // Initialize swipe button state
        binding.btnSwipe.isEnabled = currentState != BookingProgress.STATE_COMPLETE

        // Replace the existing button click listener with touch listener
        binding.btnSwipe.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isUpdating && currentState != BookingProgress.STATE_COMPLETE) {
                        initialX = event.x
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isUpdating && currentState != BookingProgress.STATE_COMPLETE) {
                        val deltaX = event.x - initialX
                        if (abs(deltaX) > SWIPE_THRESHOLD) {
                            handleProgressUpdate()
                        }
                        true
                    } else false
                }
                else -> false
            }
        }

        // Setup back button
        binding.btnBackClient.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load client details
        clientEmail?.let { email ->
            loadClientDetails(email)
        }

        // Setup progress listener
        bookingId?.let { id ->
            setupProgressListener(id)
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
                            validateStateTransition(currentState)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OngoingFragmentSProvider", "Error processing progress update", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return  // Check if fragment is still attached
                Log.e("OngoingFragmentSProvider", "Error loading progress", error.toException())
            }
        }

        // Attach new listener
        DatabaseHelper.getBookingProgressReference(bookingId)
            .addValueEventListener(progressListener!!)
    }

    private fun validateStateTransition(state: String) {
        when (state) {
            BookingProgress.STATE_PENDING -> {
                binding.btnSwipe.isEnabled = true
                binding.btnSwipe.text = "Swipe to Arrive"
            }
            BookingProgress.STATE_ARRIVE -> {
                binding.btnSwipe.isEnabled = true
                binding.btnSwipe.text = "Swipe to Start Work"
            }
            BookingProgress.STATE_WORKING -> {
                binding.btnSwipe.isEnabled = true
                binding.btnSwipe.text = "Swipe to Complete"
            }
            BookingProgress.STATE_COMPLETE -> {
                binding.btnSwipe.isEnabled = false
                binding.btnSwipe.text = "Completed"
            }
        }
    }


    private fun handleProgressUpdate() {
        if (isUpdating || currentState == BookingProgress.STATE_COMPLETE) return
        if (NetworkHelper.isNetworkAvailable.value != true) {
            // Show network error
            binding.btnSwipe.text = "No Internet Connection"
            return
        }

        bookingId?.let { id ->
            binding.btnSwipe.isEnabled = false
            isUpdating = true

            val nextState = when (currentState) {
                BookingProgress.STATE_PENDING -> BookingProgress.STATE_ARRIVE
                BookingProgress.STATE_ARRIVE -> BookingProgress.STATE_WORKING
                BookingProgress.STATE_WORKING -> BookingProgress.STATE_COMPLETE
                else -> return
            }

            try {
                val progress = BookingProgress(
                    state = nextState,
                    bookingId = id,
                    providerEmail = providerEmail ?: "",
                    clientEmail = clientEmail ?: "",
                    timestamp = System.currentTimeMillis()
                )

                DatabaseHelper.updateBookingProgress(id, progress)
                    .addOnCompleteListener { task ->
                        isUpdating = false
                        if (task.isSuccessful) {
                            if (nextState != BookingProgress.STATE_COMPLETE) {
                                binding.btnSwipe.isEnabled = true
                            }
                        } else {
                            binding.btnSwipe.isEnabled = true
                            Log.e("OngoingFragmentSProvider", "Error updating progress", task.exception)
                            updateUI(currentState)
                        }
                    }
            } catch (e: Exception) {
                isUpdating = false
                binding.btnSwipe.isEnabled = true
                Log.e("OngoingFragmentSProvider", "Error updating progress", e)
                updateUI(currentState)
            }
        }
    }


    private fun updateUI(state: String) {
        // Add loading indicator during state transitions
        binding.btnSwipe.text = if (isUpdating) {
            "Updating..."
        } else {
            when (state) {
                BookingProgress.STATE_PENDING -> "Swipe to Arrive"
                BookingProgress.STATE_ARRIVE -> "Swipe to Start Work"
                BookingProgress.STATE_WORKING -> "Swipe to Complete"
                else -> "Completed"
            }
        }

        val context = requireContext()
        val blueColor = ContextCompat.getColor(context, R.color.blue)

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
                binding.illustrationImage.setImageResource(R.drawable.sprovider_location_ongoing)
            }
            BookingProgress.STATE_ARRIVE -> {
                progressLines[0].setBackgroundColor(blueColor)
                progressIcons[1].setColorFilter(blueColor)
                binding.illustrationImage.setImageResource(R.drawable.sprovider_work_ongoing)
            }
            BookingProgress.STATE_WORKING -> {
                progressLines.take(2).forEach { it.setBackgroundColor(blueColor) }
                progressIcons.take(3).forEach { it.setColorFilter(blueColor) }
                binding.illustrationImage.setImageResource(R.drawable.sprovider_almost_work_done)
            }
            BookingProgress.STATE_COMPLETE -> {
                progressLines.forEach { it.setBackgroundColor(blueColor) }
                progressIcons.forEach { it.setColorFilter(blueColor) }
                binding.illustrationImage.setImageResource(R.drawable.sprovider_well_done_ongoing)
                binding.btnSwipe.apply {
                    isEnabled = false
                    alpha = 0.7f
                }
            }
        }
    }


    private fun loadClientDetails(email: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("email")
            .equalTo(email)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    binding.clientName.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(binding.profilePicture)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OngoingFragmentSProvider", "Error loading client details", error.toException())
            }
        })
    }

    companion object {
        private const val ARG_BOOKING_ID = "booking_id"
        private const val ARG_CLIENT_EMAIL = "client_email"
        private const val ARG_PROVIDER_EMAIL = "provider_email"  // Add this constant

        @JvmStatic
        fun newInstance(bookingId: String, clientEmail: String, providerEmail: String) =  // Update this
            OngoingFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                    putString(ARG_CLIENT_EMAIL, clientEmail)
                    putString(ARG_PROVIDER_EMAIL, providerEmail)  // Add this line
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