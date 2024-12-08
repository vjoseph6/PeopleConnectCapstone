package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.RatingsAdapter
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ActivityFragmentClient_ProviderRatings : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var ratingsAdapter: RatingsAdapter
    private val ratingsList = mutableListOf<Rating>()
    private var email: String? = null
    private var serviceType: String? = null
    private lateinit var emptyView: RelativeLayout
    private val serviceRatings = mutableMapOf<String, MutableList<Rating>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
            serviceType = it.getString("serviceType")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_client__provider_ratings, container, false)

        recyclerView = view.findViewById(R.id.rvRatings)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackClient)
        backBtn.setOnClickListener {requireActivity().supportFragmentManager.popBackStack()}

        // Modified adapter initialization to handle service-specific grouping
        ratingsAdapter = RatingsAdapter(ratingsList, true) // Add parameter for showing service
        recyclerView.adapter = ratingsAdapter

        val emptyImage: ImageView = view.findViewById(R.id.image)
        Glide.with(this)
            .load(R.drawable.nothing)
            .into(emptyImage)

        fetchRatings()

        return view
    }

    private fun fetchRatings() {
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

        ratingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ratingsList.clear()
                serviceRatings.clear()

                Log.d("SERVICE OFFERED", "$serviceType")
                // Modified to filter by serviceType if specified
                snapshot.children.forEach { ratingSnapshot ->
                    val rating = ratingSnapshot.getValue(Rating::class.java)
                    if (rating != null && rating.ratedEmail == email) {
                        // Only add ratings that match the serviceType (if specified)
                        if (serviceType == null || rating.serviceOffered == serviceType) {
                            val serviceList = serviceRatings.getOrPut(rating.serviceOffered) { mutableListOf() }
                            serviceList.add(rating)
                        }
                    }
                }

                if (ratingsList.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    if (!::ratingsAdapter.isInitialized) {
                        ratingsAdapter = RatingsAdapter(ratingsList, true)
                        recyclerView.adapter = ratingsAdapter
                    } else {
                        ratingsAdapter.notifyDataSetChanged()
                    }
                }

                // Sort services alphabetically
                val sortedServices = serviceRatings.keys.sorted()

                // Process ratings service by service
                sortedServices.forEach { service ->
                    serviceRatings[service]?.let { serviceRatingsList ->
                        // Sort ratings by timestamp (newest first)
                        val sortedRatings = serviceRatingsList.sortedByDescending { it.timestamp }

                        // Fetch user details for each rating
                        sortedRatings.forEach { rating ->
                            fetchUser(rating)
                        }
                    }
                }
            }


            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }



    private fun fetchUser(rating: Rating) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.orderByChild("email").equalTo(rating.raterEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            rating.name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                            rating.profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                        }
                    }

                    // Add rating only if it's not already in the list
                    if (!ratingsList.contains(rating)) {
                        ratingsList.add(rating)

                        // Sort the entire list by service and then by timestamp
                        ratingsList.sortWith(
                            compareBy<Rating> { it.serviceOffered }
                                .thenByDescending { it.timestamp }
                        )

                        updateAdapter()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateAdapter() {
        if (!::ratingsAdapter.isInitialized) {
            ratingsAdapter = RatingsAdapter(ratingsList, true)
            recyclerView.adapter = ratingsAdapter
        } else {
            ratingsAdapter.notifyDataSetChanged()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(email: String, serviceType: String?) =
            ActivityFragmentClient_ProviderRatings().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    serviceType?.let { putString("serviceType", it) }
                }
            }
    }
}