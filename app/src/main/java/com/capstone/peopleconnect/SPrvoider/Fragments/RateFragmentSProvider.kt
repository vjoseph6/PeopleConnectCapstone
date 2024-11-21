package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.FeedbackSelectionFragmentSProvider
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.databinding.FragmentRateSProviderBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class RateFragmentSProvider : Fragment() {
    private var bookingId: String? = null
    private var clientEmail: String? = null
    private var email: String? = null  // service provider's email
    private lateinit var binding: FragmentRateSProviderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString(ARG_BOOKING_ID)
            clientEmail = it.getString(ARG_CLIENT_EMAIL)
            email = it.getString(ARG_PROVIDER_EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRateSProviderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        setupFeedbackResultListener()
    }

    private fun setupFeedbackResultListener() {
        parentFragmentManager.setFragmentResultListener("feedback_result", viewLifecycleOwner) { _, bundle ->
            val feedback = bundle.getString("feedback") ?: ""
            submitRating(feedback)
        }
    }

    private fun setupUI() {
        // Load client details
        clientEmail?.let { email ->
            val userRef = FirebaseDatabase.getInstance().getReference("users")
            userRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            user?.let {
                                Picasso.get()
                                    .load(it.profileImageUrl)
                                    .placeholder(R.drawable.profile)
                                    .into(binding.clientImage)
                                binding.ratingQuestion.text = "How was your experience with ${it.name}?"
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RateFragmentSProvider", "Error loading user data", error.toException())
                    }
                })
        }
    }

    private fun setupListeners() {
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val description = when (rating.toInt()) {
                5 -> "Perfect!"
                4 -> "Great"
                3 -> "Good"
                2 -> "Fair"
                1 -> "Poor"
                else -> ""
            }
            binding.ratingDescription.text = description
        }

        binding.btnSubmit.setOnClickListener {
            if (binding.ratingBar.rating > 0) {
                val rating = binding.ratingBar.rating
                // For ratings 2-5, show feedback selection
                if (rating >= 2) {
                    val feedbackFragment = FeedbackSelectionFragmentSProvider.newInstance(
                        rating = rating.toString(),
                        bookingId = bookingId ?: ""
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, feedbackFragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    // For 1-star rating, submit directly
                    submitRating()
                }
            } else {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }


    // Modify the existing submitRating function
    private fun submitRating(customFeedback: String = "") {
        bookingId?.let { id ->
            // Check if already rated
            val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")
            ratingsRef.orderByChild("bookingId").equalTo(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Check if current user has already rated
                        val hasRated = snapshot.children.any {
                            it.getValue(Rating::class.java)?.raterEmail == email
                        }

                        if (hasRated) {
                            Toast.makeText(context, "You have already submitted a rating", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            return
                        }

                        // Proceed with rating submission
                        val rating = Rating(
                            bookingId = id,
                            raterEmail = email ?: "",
                            ratedEmail = clientEmail ?: "",
                            rating = binding.ratingBar.rating,
                            feedback = customFeedback.ifEmpty { binding.ratingDescription.text.toString() },
                            timestamp = System.currentTimeMillis()
                        )

                        val ratingRef = FirebaseDatabase.getInstance().getReference("ratings").push()
                        ratingRef.setValue(rating).addOnSuccessListener {
                            Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                            Log.e("RateFragmentSProvider", "Error submitting rating", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RateFragmentSProvider", "Error checking existing rating", error.toException())
                    }
                })
        }
    }

    companion object {
        private const val ARG_BOOKING_ID = "booking_id"
        private const val ARG_CLIENT_EMAIL = "client_email"
        private const val ARG_PROVIDER_EMAIL = "provider_email"

        @JvmStatic
        fun newInstance(bookingId: String, clientEmail: String, providerEmail: String) =
            RateFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                    putString(ARG_CLIENT_EMAIL, clientEmail)
                    putString(ARG_PROVIDER_EMAIL, providerEmail)
                }
            }
    }
}