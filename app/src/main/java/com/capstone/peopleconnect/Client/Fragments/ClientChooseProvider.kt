package com.capstone.peopleconnect.Client.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ProviderAdapter
import com.capstone.peopleconnect.Classes.ProviderData
import com.capstone.peopleconnect.R
import com.google.firebase.database.*
import com.bumptech.glide.Glide

class ClientChooseProvider : Fragment() {
    private lateinit var skillName: String
    private lateinit var providerAdapter: ProviderAdapter
    private val providerList = mutableListOf<ProviderData>()
    private var subCategoryName: String? = null
    private var email: String? = null
    private var bookDay: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private lateinit var emptyView: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var previousBookingsAdapter: ProviderAdapter
    private val previouslyBookedProviders = mutableListOf<ProviderData>()
    private lateinit var previousBookingsRecyclerView: RecyclerView
    private lateinit var previousBookingsTitle: TextView
    private lateinit var previousBookingsSubTitle: TextView
    private var providerEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subCategoryName = it.getString("SUBCATEGORY_NAME")
            email = it.getString("EMAIL")
            bookDay = it.getString("bookDay")
            startTime = it.getString("startTime")
            endTime = it.getString("endTime")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_choose_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.emptyView)
        recyclerView = view.findViewById(R.id.categoryRecyclerView)
        previousBookingsRecyclerView = view.findViewById(R.id.previousBookingsRecyclerView)
        previousBookingsTitle = view.findViewById(R.id.previousBookingsTitle)
        previousBookingsSubTitle = view.findViewById(R.id.previousBookingsSubtitle)

        // Initialize the empty view image with Glide animation
        val emptyImage = view.findViewById<ImageView>(R.id.image)
        Glide.with(this)
            .asGif()
            .load(R.drawable.nothing) // Make sure this is a GIF file
            .into(emptyImage)

        setupRecyclerViews(view)
        retrieveProviders()

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackClient)
        backBtn.setOnClickListener {
            val categoryFragment = CategoryFragmentClient().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("FRAGMENT_TO_LOAD", "CategoryFragmentClient")
                }
            }

            // Perform the fragment transaction with the tag and email
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, categoryFragment, "CategoryFragmentClient")
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerViews(view: View) {
        // Setup main RecyclerView
        recyclerView = view.findViewById(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        providerAdapter = ProviderAdapter(providerList) { provider ->
            navigateToProviderProfile(provider)
        }
        recyclerView.adapter = providerAdapter

        // Setup previous bookings RecyclerView
        previousBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        previousBookingsAdapter = ProviderAdapter(previouslyBookedProviders) { provider ->
            navigateToProviderProfile(provider)
        }
        previousBookingsRecyclerView.adapter = previousBookingsAdapter
    }

    private fun navigateToProviderProfile(provider: ProviderData) {
        val fragment = ActivityFragmentClient_ProviderProfile().apply {
            arguments = Bundle().apply {
                putString("NAME", provider.userName)
                Log.d("NAME SA CLIENT", provider.userName.toString())
                putString("PROFILE_IMAGE_URL", provider.imageUrl)
                putFloat("RATING", provider.rating ?: 0f)
                putInt("NO_OF_BOOKINGS", provider.noOfBookings ?: 0)
                putString("DESCRIPTION", provider.description)
                putString("PROVIDER_EMAIL", providerEmail) // Use the stored providerEmail
                putString("SERVICE_OFFERED", subCategoryName)
                putString("bookDay", bookDay)
                putString("startTime", startTime)
                putString("endTime", endTime)
                putString("LAST_BOOKING_DATE", provider.lastBookingDate)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun retrieveProviders() {
        providerList.clear()
        previouslyBookedProviders.clear()

        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("bookByEmail").equalTo(email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val previouslyBookedEmails = mutableMapOf<String, String>()

                    for (bookingSnapshot in snapshot.children) {
                        val serviceOffered = bookingSnapshot.child("serviceOffered").getValue(String::class.java)
                        val bookingStatus = bookingSnapshot.child("bookingStatus").getValue(String::class.java)

                        if (serviceOffered == subCategoryName && bookingStatus == "Completed") {
                            // Store the providerEmail at class level
                            providerEmail = bookingSnapshot.child("providerEmail").getValue(String::class.java)
                            val bookingDate = bookingSnapshot.child("bookingDay").getValue(String::class.java)

                            if (providerEmail != null && bookingDate != null) {
                                if (!previouslyBookedEmails.containsKey(providerEmail) ||
                                    bookingDate > previouslyBookedEmails[providerEmail]!!) {
                                    previouslyBookedEmails[providerEmail!!] = bookingDate
                                }
                            }
                        }
                    }

                    retrieveAllProviders(previouslyBookedEmails)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Bookings", "Error: ${error.message}")
                }
            })
    }


    private fun retrieveAllProviders(previouslyBookedEmails: Map<String, String>) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")
        
        skillsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                providerList.clear()
                previouslyBookedProviders.clear()

                for (skillSetSnapshot in snapshot.children) {
                    val user = skillSetSnapshot.child("user").getValue(String::class.java)
                    
                    if (user == email) continue

                    processSkillSnapshot(skillSetSnapshot, user, previouslyBookedEmails)
                }

                // Log the results
                Log.d("Providers", "Previous bookings: ${previouslyBookedProviders.size}")
                Log.d("Providers", "New providers: ${providerList.size}")

                updateVisibility()
                providerAdapter.notifyDataSetChanged()
                previousBookingsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SkillData", "Error: ${error.message}")
            }
        })
    }

    private fun processSkillSnapshot(
        skillSetSnapshot: DataSnapshot, 
        userEmail: String?, 
        previouslyBookedEmails: Map<String, String>
    ) {
        val skillItemsSnapshot = skillSetSnapshot.child("skillItems")
        
        if (skillItemsSnapshot.exists()) {
            for (skillSnapshot in skillItemsSnapshot.children) {
                val skillName = skillSnapshot.child("name").getValue(String::class.java)
                if (skillName == subCategoryName) {
                    val isVisible = skillSnapshot.child("visible").getValue(Boolean::class.java) ?: false
                    if (isVisible) {
                        // Log the skill rate for debugging
                        val rate = skillSnapshot.child("skillRate").getValue(Int::class.java)
                        Log.d("SkillRate", "SkillRate for $skillName: $rate")
                        
                        userEmail?.let { email ->
                            retrieveUserDetails(
                                email,
                                skillSnapshot,
                                previouslyBookedEmails[email]
                            )
                        }
                    }
                }
            }
        }
    }

    private fun retrieveUserDetails(
        userEmail: String,
        skillSnapshot: DataSnapshot,
        lastBookingDate: String?
    ) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")
        
        userRef.orderByChild("email").equalTo(userEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val providerData = createProviderData(userSnapshot, skillSnapshot, lastBookingDate)
                            
                            if (lastBookingDate != null) {
                                previouslyBookedProviders.add(providerData)
                            } else {
                                providerList.add(providerData)
                            }
                        }
                        updateVisibility()
                        providerAdapter.notifyDataSetChanged()
                        previousBookingsAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserData", "Error: ${error.message}")
                }
            })
    }

    private fun updateVisibility() {
        if (previouslyBookedProviders.isEmpty() && providerList.isEmpty()) {
            // Show empty view if both lists are empty
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            previousBookingsRecyclerView.visibility = View.GONE
            previousBookingsTitle.visibility = View.GONE
        } else {
            // Hide empty view
            emptyView.visibility = View.GONE
            
            // Show/hide previous bookings section
            if (previouslyBookedProviders.isEmpty()) {
                previousBookingsRecyclerView.visibility = View.GONE
                previousBookingsTitle.visibility = View.GONE
                previousBookingsSubTitle.visibility = View.GONE
            } else {
                previousBookingsRecyclerView.visibility = View.VISIBLE
                previousBookingsTitle.visibility = View.VISIBLE
                previousBookingsSubTitle.visibility = View.VISIBLE
            }
            
            // Always show main recycler view if there are any providers
            recyclerView.visibility = if (providerList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun createProviderData(
        userSnapshot: DataSnapshot,
        skillSnapshot: DataSnapshot,
        lastBookingDate: String?
    ): ProviderData {
        // Log the data for debugging
        val rate = skillSnapshot.child("skillRate").getValue(Int::class.java)
        Log.d("ProviderData", "SkillRate from snapshot: $rate")
        
        return ProviderData(
            userName = userSnapshot.child("name").getValue(String::class.java),
            name = skillSnapshot.child("name").getValue(String::class.java),
            imageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java),
            skillRate = rate,
            description = skillSnapshot.child("description").getValue(String::class.java),
            rating = skillSnapshot.child("rating").getValue(Float::class.java) ?: 0f,
            noOfBookings = userSnapshot.child("noOfBookings").getValue(Int::class.java) ?: 0,
            lastBookingDate = lastBookingDate
        )
    }

    companion object {
        fun newInstance(subCategoryName: String, email: String, bookDay: String, startTime: String, endTime: String) =
            ClientChooseProvider().apply {
                arguments = Bundle().apply {
                    putString("SUBCATEGORY_NAME", subCategoryName)
                    putString("EMAIL", email)
                    putString("bookDay", bookDay)
                    putString("startTime", startTime)
                    putString("endTime", endTime)
                }
            }
    }
}