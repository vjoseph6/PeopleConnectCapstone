package com.capstone.peopleconnect.SPrvoider.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
    private var swipeThumb: ImageView? = null
    private var swipeText: TextView? = null
    private var swipeProgress: View? = null
    private var swipeBackground: ConstraintLayout? = null
    private var initialTouchX = 0f
    private val swipeThreshold = 0.7f

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


        // Initialize swipe button views
        swipeThumb = binding.btnSwipe.root.findViewById(R.id.swipe_thumb)
        swipeText = binding.btnSwipe.root.findViewById(R.id.swipe_text)
        swipeProgress = binding.btnSwipe.root.findViewById(R.id.swipe_progress)
        swipeBackground = binding.btnSwipe.root.findViewById(R.id.swipe_button_background)


        // Add network monitoring
        NetworkHelper.startNetworkMonitoring(requireContext())
        NetworkHelper.isNetworkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            swipeBackground?.isEnabled = isAvailable && currentState != BookingProgress.STATE_COMPLETE
            if (!isAvailable) {
                swipeText?.text = "No Internet Connection"
            } else {
                validateStateTransition(currentState)
            }
        }


        swipeBackground?.isEnabled = currentState != BookingProgress.STATE_COMPLETE

        swipeThumb?.setOnTouchListener { view, event ->
            if (!isUpdating && currentState != BookingProgress.STATE_COMPLETE && swipeBackground?.isEnabled == true) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialX = view.x
                        swipeProgress?.visibility = View.VISIBLE
                        view.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .start()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val moved = event.rawX - initialTouchX
                        val maxSlide = (swipeBackground?.width ?: 0) - view.width
                        val newX = (initialX + moved).coerceIn(0f, maxSlide.toFloat())
                        view.x = newX

                        // Update progress width with animation
                        swipeProgress?.animate()
                            ?.alpha(0.8f)
                            ?.setDuration(0)
                            ?.withStartAction {
                                swipeProgress?.layoutParams?.width = newX.toInt() + view.width
                                swipeProgress?.requestLayout()
                            }
                            ?.start()

                        // Update text alpha based on progress with smooth transition
                        val slidePercentage = newX / maxSlide
                        swipeText?.animate()
                            ?.alpha(1 - slidePercentage)
                            ?.setDuration(0)
                            ?.start()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()

                        val maxSlide = (swipeBackground?.width ?: 0) - view.width
                        val slidePercentage = view.x / maxSlide

                        if (slidePercentage > swipeThreshold) {
                            // Complete the swipe animation with spring effect
                            view.animate()
                                .x(maxSlide.toFloat())
                                .setDuration(200)
                                .withEndAction {
                                    handleProgressUpdate()
                                    resetSwipeButton()
                                }
                                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                                .start()
                        } else {
                            // Reset with spring effect
                            resetSwipeButton()
                        }
                        true
                    }
                    else -> false
                }
            } else false
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
            checkBookingStatus()  // Add this line
        }

    }

    private fun checkBookingStatus() {
        bookingId?.let { id ->
            val bookingRef = FirebaseDatabase.getInstance().getReference("bookings/$id")
            bookingRef.child("bookingStatus").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status == "Completed" && !isUpdating) {
                        // Check if fragment is still attached
                        if (!isAdded) return

                        // Navigate to rating screen
                        val rateFragment = RateFragmentSProvider.newInstance(
                            bookingId = id,
                            clientEmail = clientEmail ?: "",
                            providerEmail = providerEmail ?: ""
                        )
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, rateFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OngoingFragmentSProvider", "Error checking booking status", error.toException())
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
                swipeBackground?.isEnabled = true
                swipeText?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeText?.text = "Swipe to Arrive"
                        swipeText?.animate()
                            ?.alpha(1f)
                            ?.setDuration(150)
                            ?.start()
                    }?.start()
            }
            BookingProgress.STATE_ARRIVE -> {
                swipeBackground?.isEnabled = true
                swipeText?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeText?.text = "Swipe to Start Work"
                        swipeText?.animate()
                            ?.alpha(1f)
                            ?.setDuration(150)
                            ?.start()
                    }?.start()
            }
            BookingProgress.STATE_WORKING -> {
                swipeBackground?.isEnabled = true
                swipeText?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeText?.text = "Swipe to Completed"
                        swipeText?.animate()
                            ?.alpha(1f)
                            ?.setDuration(150)
                            ?.start()
                    }?.start()
            }
            "AWAITING_CLIENT_CONFIRMATION" -> {
                swipeBackground?.isEnabled = false
                swipeText?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeText?.text = "Waiting for client confirmation..."
                        swipeText?.animate()
                            ?.alpha(1f)
                            ?.setDuration(150)
                            ?.start()
                    }?.start()
            }
            BookingProgress.STATE_COMPLETE -> {
                swipeBackground?.isEnabled = false
                swipeText?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeText?.text = "Completed"
                        swipeText?.animate()
                            ?.alpha(1f)
                            ?.setDuration(150)
                            ?.start()
                        swipeBackground?.animate()
                            ?.alpha(0.7f)
                            ?.setDuration(200)
                            ?.start()
                    }?.start()
            }
        }
    }
    private fun resetSwipeButton() {
        swipeThumb?.animate()
            ?.x(0f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
            ?.withEndAction {
                swipeText?.animate()
                    ?.alpha(1f)
                    ?.setDuration(150)
                    ?.start()
                swipeProgress?.animate()
                    ?.alpha(0f)
                    ?.setDuration(150)
                    ?.withEndAction {
                        swipeProgress?.visibility = View.INVISIBLE
                        swipeProgress?.layoutParams?.width = 0
                        swipeProgress?.requestLayout()
                    }
                    ?.start()
            }
            ?.start()
    }


    private fun handleProgressUpdate() {
        if (isUpdating || currentState == BookingProgress.STATE_COMPLETE) return
        if (NetworkHelper.isNetworkAvailable.value != true) {
            swipeText?.text = "No Internet Connection"
            return
        }

        bookingId?.let { id ->
            swipeBackground?.isEnabled = false
            isUpdating = true

            val nextState = when (currentState) {
                BookingProgress.STATE_PENDING -> BookingProgress.STATE_ARRIVE
                BookingProgress.STATE_ARRIVE -> BookingProgress.STATE_WORKING
                BookingProgress.STATE_WORKING -> BookingProgress.STATE_AWAITING_CONFIRMATION
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
                            when (nextState) {
                                BookingProgress.STATE_AWAITING_CONFIRMATION -> {
                                    swipeBackground?.isEnabled = false
                                    swipeText?.text = "Waiting for client confirmation..."
                                }
                                BookingProgress.STATE_COMPLETE -> {
                                    // Navigate to rating screen when booking is completed
                                    val rateFragment = RateFragmentSProvider.newInstance(
                                        bookingId = id,
                                        clientEmail = clientEmail ?: "",
                                        providerEmail = providerEmail ?: ""
                                    )
                                    parentFragmentManager.beginTransaction()
                                        .replace(R.id.frame_layout, rateFragment)
                                        .addToBackStack(null)
                                        .commit()
                                }
                                else -> {
                                    swipeBackground?.isEnabled = true
                                    resetSwipeButton()
                                }
                            }
                            updateUI(nextState)
                        } else {
                            swipeBackground?.isEnabled = true
                            Log.e("OngoingFragmentSProvider", "Error updating progress", task.exception)
                            updateUI(currentState)
                            resetSwipeButton()
                        }
                    }
            } catch (e: Exception) {
                isUpdating = false
                swipeBackground?.isEnabled = true
                Log.e("OngoingFragmentSProvider", "Error updating progress", e)
                updateUI(currentState)
                resetSwipeButton()
            }
        }
    }




    private fun updateUI(state: String) {
        swipeText?.text = if (isUpdating) {
            "Updating..."
        } else {
            when (state) {
                BookingProgress.STATE_PENDING -> "Swipe to Arrive"
                BookingProgress.STATE_ARRIVE -> "Swipe to Start Work"
                BookingProgress.STATE_WORKING -> "Swipe to Completed"
                "AWAITING_CLIENT_CONFIRMATION" -> "Waiting for client confirmation..."
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
                swipeBackground?.apply {
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