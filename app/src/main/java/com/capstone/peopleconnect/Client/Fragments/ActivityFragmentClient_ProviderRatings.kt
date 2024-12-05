package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.RatingsAdapter
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log

class ActivityFragmentClient_ProviderRatings : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var ratingsAdapter: RatingsAdapter
    private val ratingsList = mutableListOf<Rating>()
    private var email: String? = null
    private var selectedService: String? = null  // Add this line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
            selectedService = it.getString("SERVICE")  // Add this line
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_client__provider_ratings, container, false)

        recyclerView = view.findViewById(R.id.rvRatings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        ratingsAdapter = RatingsAdapter(ratingsList)
        recyclerView.adapter = ratingsAdapter

        fetchRatings()

        return view
    }

    private fun fetchRatings() {
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

        ratingsRef.orderByChild("ratedEmail").equalTo(email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ratingsList.clear()

                    for (ratingSnapshot in snapshot.children) {
                        val rating = ratingSnapshot.getValue(Rating::class.java)
                        if (rating != null && rating.serviceOffered == selectedService) {
                            fetchUser(rating)  // Only call fetchUser once
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProviderRatings", "Error fetching ratings", error.toException())
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
                    ratingsList.add(rating)
                    updateAdapter()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateAdapter() {
        if (!::ratingsAdapter.isInitialized) {
            ratingsAdapter = RatingsAdapter(ratingsList)
            recyclerView.adapter = ratingsAdapter
        } else {
            ratingsAdapter.notifyDataSetChanged()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(email: String, service: String) =  // Update this line
            ActivityFragmentClient_ProviderRatings().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("SERVICE", service)  // Add this line
                }
            }
    }
}
