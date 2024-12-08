package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.RatingsAdapter
import com.capstone.peopleconnect.Adapters.RatingsClientAdapter
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso


class ActivityFragmentSProvider_ClientRatings : Fragment() {
    private var email: String? = null
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var clientNameTextView: TextView
    private val ratingsList = mutableListOf<Rating>()
    private lateinit var clientRatingsTextView: TextView
    private lateinit var ratingsRecyclerView: RecyclerView
    private lateinit var ratingsAdapter: RatingsClientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_activity_s_provider__client_ratings,
            container,
            false
        )


        // Initialize views
        ratingsRecyclerView = view.findViewById(R.id.rvRatingsProvider)
        profileImageView = view.findViewById(R.id.profileImage)
        clientNameTextView = view.findViewById(R.id.clientName)
        clientRatingsTextView = view.findViewById(R.id.clientRatings)

        // Setup RecyclerView
        ratingsAdapter = RatingsClientAdapter(ratingsList, true)
        ratingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        ratingsRecyclerView.adapter = ratingsAdapter

        // Back button
        val backBtn: ImageButton = view.findViewById(R.id.btnBackClient)
        backBtn.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val emptyImage: ImageView = view.findViewById(R.id.image)
        Glide.with(this)
            .load(R.drawable.nothing)
            .into(emptyImage)

        // Fetch user and ratings
        email?.let {
            fetchUserProfile(it)
            fetchRatings(it)
        }

        return view
    }

    private fun fetchRatings(email: String) {
        val ratingsRef = FirebaseDatabase.getInstance().reference
            .child("ratings")
            .orderByChild("ratedEmail")
            .equalTo(email)

        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear previous ratings
                ratingsList.clear()

                if (!snapshot.exists()) {
                    updateRatingsUI(emptyList())
                    return
                }

                // Process each rating
                val ratingsToProcess = snapshot.children.toList()
                processRatings(ratingsToProcess)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching ratings: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun processRatings(ratingsSnapshots: List<DataSnapshot>) {
        val processedRatings = mutableListOf<Rating>()

        // Create a counter to track processing
        val totalRatings = ratingsSnapshots.size
        var processedCount = 0

        ratingsSnapshots.forEach { ratingSnapshot ->
            // Extract rating details
            val raterEmail = ratingSnapshot.child("raterEmail").value?.toString() ?: return@forEach
            val ratingValue = ratingSnapshot.child("rating").value?.toString()?.toFloatOrNull() ?: 0f
            val serviceOffered = ratingSnapshot.child("serviceOffered").value?.toString() ?: ""
            val feedback = ratingSnapshot.child("feedback").value?.toString() ?: ""
            val timestamp = ratingSnapshot.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L

            // Fetch rater's detailed information
            fetchRaterDetails(
                raterEmail,
                ratingValue,
                serviceOffered,
                feedback,
                timestamp
            ) { raterInfo ->
                processedRatings.add(raterInfo)
                processedCount++

                // When all ratings are processed
                if (processedCount == totalRatings) {
                    // Sort ratings by timestamp (most recent first)
                    val sortedRatings = processedRatings.sortedByDescending { it.timestamp }
                    updateRatingsUI(sortedRatings)
                }
            }
        }
    }

    private fun fetchRaterDetails(
        raterEmail: String,
        ratingValue: Float,
        serviceOffered: String,
        feedback:String,
        timestamp: Long,
        onComplete: (Rating) -> Unit
    ) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.orderByChild("email").equalTo(raterEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.first()
                        val profileImageUrl = userSnapshot.child("profileImageUrl").value?.toString() ?: ""
                        val name = userSnapshot.child("name").value?.toString() ?: "Unknown"

                        val rating = Rating(
                            name = name,
                            rating = ratingValue,
                            serviceOffered = serviceOffered,
                            profileImageUrl = profileImageUrl,
                            raterEmail = raterEmail,
                            timestamp = timestamp,
                            feedback = feedback
                        )

                        onComplete(rating)
                    } else {
                        // Create a rating with default values if user not found
                        val rating = Rating(
                            name = "Unknown",
                            rating = ratingValue,
                            serviceOffered = serviceOffered,
                            timestamp = timestamp
                        )
                        onComplete(rating)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Create a rating with default values if query is cancelled
                    val rating = Rating(
                        name = "Unknown",
                        rating = ratingValue,
                        serviceOffered = serviceOffered,
                        timestamp = timestamp
                    )
                    onComplete(rating)
                }
            })
    }

    private fun updateRatingsUI(ratings: List<Rating>) {
        val emptyView: RelativeLayout = view?.findViewById(R.id.emptyView) ?: return
        val recyclerView: RecyclerView = ratingsRecyclerView

        if (ratings.isEmpty()) {
            // Show empty view and hide RecyclerView
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            clientRatingsTextView.text = "★ 0.0 (0 Reviews)"
        } else {
            // Hide empty view and show RecyclerView
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            // Update adapter
            ratingsAdapter.updateRatings(ratings)

            // Calculate and update overall rating
            val averageRating = ratings.map { it.rating }.average()
            val roundedRating = String.format("%.1f", averageRating)
            clientRatingsTextView.text = "★ $roundedRating (${ratings.size} Reviews)"
        }
    }


    private fun fetchUserProfile(email: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.first()

                        val profileImageUrl = userSnapshot.child("profileImageUrl").value?.toString() ?: ""
                        val name = userSnapshot.child("name").value?.toString() ?: "Unknown"

                        // Load profile image
                        Picasso.get().load(profileImageUrl)
                            .placeholder(R.drawable.profile1)
                            .into(profileImageView)

                        // Set name
                        clientNameTextView.text = name
                    } else {
                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    companion object {
        @JvmStatic
        fun newInstance(email: String) =
            ActivityFragmentSProvider_ClientRatings().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}