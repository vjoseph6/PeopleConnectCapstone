package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.FeedbackSelectionFragment
import com.capstone.peopleconnect.Helper.DatabaseHelper
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.databinding.FragmentRateClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso


class RateFragmentClient : Fragment() {
    private var bookingId: String? = null
    private var providerEmail: String? = null
    private var email: String? = null  // Add this line
    private lateinit var binding: FragmentRateClientBinding

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
        binding = FragmentRateClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        setupFeedbackResultListener()
    }

    // Add this new function
    private fun setupFeedbackResultListener() {
        parentFragmentManager.setFragmentResultListener("feedback_result", viewLifecycleOwner) { _, bundle ->
            val feedback = bundle.getString("feedback") ?: ""
            submitRating(feedback)  // Pass the feedback to submitRating
        }
    }

    private fun setupUI() {
        // Load provider details
        providerEmail?.let { email ->
            val userRef = FirebaseDatabase.getInstance().getReference("users")
            userRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            user?.let {
                                // Load image using Picasso
                                Picasso.get()
                                    .load(it.profileImageUrl)
                                    .placeholder(R.drawable.profile1)
                                    .into(binding.providerImage)
                                binding.ratingQuestion.text = "How was ${it.name}'s service?"
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RateFragmentClient", "Error loading user data", error.toException())
                    }
                })
        }
    }

    private fun setupListeners() {

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


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
                // For ratings 2-5, show feedback selection
                if (binding.ratingBar.rating >= 2) {
                    navigateToFeedbackSelection()
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

    private fun navigateToFeedbackSelection() {
        val feedbackFragment = FeedbackSelectionFragment.newInstance(
            bookingId ?: "",
            binding.ratingBar.rating.toString()
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, feedbackFragment)
            .addToBackStack(null)
            .commit()
    }

    // Add this check before submitting rating
    private fun submitRating(customFeedback: String = "") {
        bookingId?.let { id ->
            val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")
            ratingsRef.orderByChild("bookingId").equalTo(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val hasRated = snapshot.children.any {
                            it.getValue(Rating::class.java)?.raterEmail == email
                        }

                        if (hasRated) {
                            Toast.makeText(context, "You have already submitted a rating", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            return
                        }

                        val rating = Rating(
                            bookingId = id,
                            raterEmail = email ?: "",
                            ratedEmail = providerEmail ?: "",
                            rating = binding.ratingBar.rating,
                            feedback = customFeedback.ifEmpty { binding.ratingDescription.text.toString() },
                            timestamp = System.currentTimeMillis()
                        )

                        val ratingRef = ratingsRef.push()
                        ratingRef.setValue(rating).addOnSuccessListener {
                            Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
                            // Call updateUserRating after successfully submitting the rating
                            updateUserRating(providerEmail ?: "", id) // Pass the rater email and booking ID
                            parentFragmentManager.popBackStack()
                        }.addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                            Log.e("RateFragmentClient", "Error submitting rating", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RateFragmentClient", "Error checking existing rating", error.toException())
                    }
                })
        }
    }

    private fun updateUserRating(raterEmail: String, bookingId: String) {
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        // Step 1: Fetch the serviceOffered from bookings
        bookingsRef.orderByChild("bookingId").equalTo(bookingId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(bookingSnapshot: DataSnapshot) {
                    if (bookingSnapshot.exists()) {
                        Log.d("UpdateUserRating", "Booking found for ID: $bookingId")

                        for (booking in bookingSnapshot.children) {
                            val serviceOffered = booking.child("serviceOffered").getValue(String::class.java) ?: ""
                            Log.d("UpdateUserRating", "Service offered: $serviceOffered")

                            // Step 2: Iterate over skills to find matching skill and user
                            skillsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(skillSnapshot: DataSnapshot) {
                                    var updated = false

                                    for (skill in skillSnapshot.children) {
                                        val skillItems = skill.child("skillItems")
                                        val userEmail = skill.child("user").getValue(String::class.java) ?: ""

                                        Log.d("UpdateUserRating", "Checking skills for user: $userEmail")

                                        if (userEmail.equals(raterEmail, ignoreCase = true)) {
                                            Log.d("UpdateUserRating", "Matching user found: $userEmail")

                                            for (item in skillItems.children) {
                                                val skillName = item.child("name").getValue(String::class.java) ?: ""
                                                Log.d("UpdateUserRating", "Checking skill: $skillName")

                                                if (skillName.equals(serviceOffered, ignoreCase = true)) {
                                                    Log.d("UpdateUserRating", "Matching skill found: $skillName")
                                                    updated = true

                                                    // Step 3: Get existing SkillItem and update its fields
                                                    val skillItem = item.getValue(SkillItem::class.java)
                                                    if (skillItem != null) {
                                                        Log.d("UpdateUserRating", "Skill item found: $skillItem")

                                                        // Update skill item properties
                                                        skillItem.noOfBookings += 1
                                                        skillItem.totalRating += binding.ratingBar.rating
                                                        skillItem.rating = skillItem.totalRating / skillItem.noOfBookings

                                                        Log.d("UpdateUserRating", "Updated skill item: $skillItem")

                                                        // Save updated skill item back to Firebase
                                                        item.ref.setValue(skillItem)
                                                            .addOnSuccessListener {
                                                                Log.d("UpdateUserRating", "Skill rating updated successfully")
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("UpdateUserRating", "Error updating skill rating", e)
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!updated) {
                                        Log.w("UpdateUserRating", "No matching skill or user found for the provided data")
                                        Toast.makeText(context, "No matching skill or user found for the provided data", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("UpdateUserRating", "Error fetching skills data", error.toException())
                                }
                            })
                        }
                    } else {
                        Log.w("UpdateUserRating", "No booking found with ID: $bookingId")
                        Toast.makeText(context, "No booking found with ID: $bookingId", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UpdateUserRating", "Error fetching booking data", error.toException())
                }
            })
    }





    companion object {
        private const val ARG_BOOKING_ID = "booking_id"
        private const val ARG_PROVIDER_EMAIL = "provider_email"
        private const val ARG_CLIENT_EMAIL = "client_email"  // Add this line

        @JvmStatic
        fun newInstance(bookingId: String, providerEmail: String, clientEmail: String) =  // Update parameters
            RateFragmentClient().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                    putString(ARG_PROVIDER_EMAIL, providerEmail)
                    putString(ARG_CLIENT_EMAIL, clientEmail)  // Add this line
                }
            }
    }
}