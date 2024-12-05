package com.capstone.peopleconnect.Client.Fragments

import SkillsPostsAdapter
import android.app.Dialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoryAdapter
import com.capstone.peopleconnect.Adapters.ProviderServicesAdapter
import com.capstone.peopleconnect.Adapters.RatingsAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.capstone.peopleconnect.Classes.Rating


class ActivityFragmentClient_ProviderProfile : Fragment() {

    private lateinit var providerNameTextView: TextView
    private lateinit var providerRatingTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var providerDescription: TextView
    private lateinit var email: String
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView
    private var bookDay: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private var serviceOffered: String = ""
    private lateinit var servicesAdapter: ProviderServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_activity_client__provider_profile, container, false)

        // Find views by ID
        providerNameTextView = view.findViewById(R.id.providerName)
        providerRatingTextView = view.findViewById(R.id.providerRating)
        profileImageView = view.findViewById(R.id.profileImage)
        providerDescription = view.findViewById(R.id.experienceDescription)

        val btnBackClient: ImageButton = view.findViewById(R.id.btnBackClient)
        btnBackClient.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Retrieve data from arguments
        val providerName = arguments?.getString("NAME") ?: "Default Name"
        val profileImageUrl = arguments?.getString("PROFILE_IMAGE_URL") ?: ""
        val rating = arguments?.getFloat("RATING") ?: 0f
        val noOfBookings = arguments?.getString("NO_OF_BOOKINGS") ?: "0"
        val description = arguments?.getString("DESCRIPTION") ?: ""
        serviceOffered = arguments?.getString("SERVICE_OFFERED") ?: ""
        bookDay = arguments?.getString("bookDay") ?: ""
        startTime = arguments?.getString("startTime") ?: ""
        endTime = arguments?.getString("endTime") ?: ""


        val bookNowButton: ImageButton = view.findViewById(R.id.bookNowButton)
        bookNowButton.setOnClickListener {
            val bookingFragment = ActivityFragmentClient_BookDetails().apply {
                arguments = Bundle().apply {
                    putString("NAME", providerName)
                    putString("SERVICE_OFFERED", serviceOffered)
                    putString("bookDay", bookDay)
                    putString("startTime", startTime)
                    putString("endTime", endTime)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, bookingFragment)
                .addToBackStack(null)
                .commit()
        }


        recyclerView = view.findViewById(R.id.servicesRecyclerView)
        setupRecyclerView()

        fetchUser(providerName)

        // Set the values to the respective views
        providerNameTextView.text = providerName
        providerDescription.text = description
        providerRatingTextView.text = "★ $rating ($noOfBookings Bookings)"

        // Set the profile image (You can use Picasso or Glide for image loading)
        if (!profileImageUrl.isNullOrEmpty()) {
            Picasso.get().load(profileImageUrl).into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.profile)
        }

        // Check for the tag and adjust UI accordingly
        val tag = arguments?.getString("TAG")
        if (tag == "fromApplicants") {
            bookNowButton.visibility = View.GONE
        }

        // Set up click listener for providerRatingTextView
        providerRatingTextView.setOnClickListener {
            navigateToProviderRatings(email)
            Log.d("Email passed in ", "$email")
        }

        return view
    }

    private fun navigateToProviderRatings(email: String) {
        val providerRatingsFragment = ActivityFragmentClient_ProviderRatings.newInstance(email, serviceOffered)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, providerRatingsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun fetchUser(providerName: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.orderByChild("name").equalTo(providerName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "User data exists for: $providerName")
                        for (userSnapshot in snapshot.children) {
                            email = userSnapshot.child("email").getValue(String::class.java).toString()
                            val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java).toString()
                            Picasso.get().load(profileImageUrl).into(profileImageView)
                            Log.d(TAG, "Retrieved email: $email")
                            if (email != null) {
                                fetchSkills(email)
                                fetchWorks(email)
                            }
                        }
                    } else {
                        Log.d(TAG, "No user data found for: $providerName")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                }
            })
    }

    private fun fetchSkills(email: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

        skillsRef.orderByChild("user").equalTo(email).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Skills data exists for user: $email")
                    val categories = mutableListOf<Category>()
                    var totalItems = 0
                    var fetchedItems = 0

                    // First, fetch all ratings for this provider
                    ratingsRef.orderByChild("ratedEmail").equalTo(email)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ratingsSnapshot: DataSnapshot) {
                                // Group ratings by service
                                val serviceRatings = mutableMapOf<String, MutableList<Float>>()
                                val serviceBookings = mutableMapOf<String, Int>() // Track number of ratings per service

                                // Collect all ratings grouped by service
                                ratingsSnapshot.children.forEach { ratingSnapshot ->
                                    val rating = ratingSnapshot.getValue(Rating::class.java)
                                    if (rating != null) {
                                        // Add rating to service-specific list
                                        val ratingsList = serviceRatings.getOrPut(rating.serviceOffered) { mutableListOf() }
                                        ratingsList.add(rating.rating)

                                        // Increment booking count for this service
                                        serviceBookings[rating.serviceOffered] = (serviceBookings[rating.serviceOffered] ?: 0) + 1
                                    }
                                }

                                // Now process skills with their corresponding ratings
                                for (skillSnapshot in snapshot.children) {
                                    totalItems += skillSnapshot.child("skillItems").childrenCount.toInt()
                                    val skillItemsSnapshot = skillSnapshot.child("skillItems")

                                    for (itemSnapshot in skillItemsSnapshot.children) {
                                        val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                                        val description = itemSnapshot.child("description").getValue(String::class.java) ?: ""

                                        // Get ratings only for this specific service
                                        val serviceRatingsList = serviceRatings[name] ?: listOf()
                                        val numBookings = serviceBookings[name] ?: 0 // Get number of ratings for this service

                                        // Calculate average rating for this specific service
                                        val averageRating = if (serviceRatingsList.isNotEmpty()) {
                                            serviceRatingsList.average().toFloat()
                                        } else {
                                            0.0f
                                        }

                                        // Update the skill item's rating and bookings in Firebase
                                        itemSnapshot.ref.child("rating").setValue(averageRating)
                                        itemSnapshot.ref.child("noOfBookings").setValue(numBookings)

                                        // If this is the selected service, update the UI with service-specific rating
                                        if (serviceOffered == name) {
                                            providerDescription.text = description
                                            providerRatingTextView.text = "★ ${String.format("%.1f", averageRating)} ($numBookings Reviews)"
                                            Log.d(TAG, "Match found! Service: $name, Rating: $averageRating, Reviews: $numBookings")
                                        }

                                        // Fetch image and create category
                                        fetchImage(name) { imageUrl ->
                                            val category = Category(
                                                name = name,
                                                image = imageUrl,
                                            )
                                            categories.add(category)
                                            Log.d(TAG, "Fetched image URL for $name: $imageUrl")

                                            fetchedItems++
                                            if (fetchedItems == totalItems) {
                                                servicesAdapter.updateServices(categories)
                                                servicesAdapter.updateSelectedService(serviceOffered)
                                            }
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Error fetching ratings", error.toException())
                            }
                        })
                } else {
                    Log.d(TAG, "No skills data found for user: $email")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
            }
        })
    }

    private fun fetchWorks(email: String) {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val approvedImages = mutableListOf<String>()

        postsRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (postSnapshot in snapshot.children) {
                            val postStatus = postSnapshot.child("postStatus").getValue(String::class.java)
                            if (postStatus == "Approved") {
                                val imagesList = postSnapshot.child("postImages").children.mapNotNull {
                                    it.getValue(String::class.java)
                                }
                                approvedImages.addAll(imagesList) // Add all approved images
                            }
                        }
                        // Pass the collected images to the RecyclerView adapter
                        setupRecyclerView(approvedImages)
                    } else {
                        Log.d(TAG, "No approved posts found for email: $email")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                }
            })
    }

    private fun setupRecyclerView(postImages: List<String>) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.worksRecyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView?.adapter = SkillsPostsAdapter(postImages) { imageUrl ->
            val fullScreenDialog = Dialog(requireContext())
            fullScreenDialog.setContentView(R.layout.dialog_fullscreen_image)
            val fullScreenImageView = fullScreenDialog.findViewById<ImageView>(R.id.fullscreenImageView)

            // Load the image into the full-screen view using Picasso
            Picasso.get().load(imageUrl).into(fullScreenImageView)

            // Show the full-screen dialog
            fullScreenDialog.show()
        }
    }




    private fun fetchImage(name: String, callback: (String) -> Unit) {
        val categoryRef = FirebaseDatabase.getInstance().reference

        categoryRef.child("category").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (category in snapshot.children) {
                    val subCategoriesSnapshot = category.child("Sub Categories")

                    // Search in each subcategory for the matching skill name
                    for (subCategory in subCategoriesSnapshot.children) {
                        val subName = subCategory.child("name").getValue(String::class.java)
                        val image = subCategory.child("image").getValue(String::class.java)

                        if (subName == name) {
                            // Found the image for the skill, return it via the callback
                            Log.d(TAG, "Match Found: $subName, Image: $image")
                            callback(image ?: "")
                            return // Exit loop once a match is found
                        }
                    }
                }
                // If no match found, return an empty string
                callback("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                callback("") // Pass an empty string if there's an error
            }
        })
    }

    private fun setupRecyclerView() {
        servicesAdapter = ProviderServicesAdapter(
            services = mutableListOf(),
            selectedService = serviceOffered
        ) { newService ->
            serviceOffered = newService
            servicesAdapter.updateSelectedService(newService)

            // Fetch ratings for the selected service
            val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")
            ratingsRef.orderByChild("ratedEmail").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Count ratings and calculate average for this specific service
                        var totalRating = 0f
                        var numRatings = 0

                        snapshot.children.forEach { ratingSnapshot ->
                            val rating = ratingSnapshot.getValue(Rating::class.java)
                            if (rating != null && rating.serviceOffered == newService) {
                                totalRating += rating.rating
                                numRatings++
                            }
                        }

                        // Update the rating display for this service
                        val averageRating = if (numRatings > 0) totalRating / numRatings else 0f
                        providerRatingTextView.text = "★ ${String.format("%.1f", averageRating)} ($numRatings Reviews)"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error fetching service ratings", error.toException())
                    }
                })

            // Re-fetch skills to update the description based on the selected service
            fetchSkills(email)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = servicesAdapter
        }
    }

    companion object {
        private const val ARG_NAME = "NAME"
        private const val ARG_PROFILE_IMAGE_URL = "PROFILE_IMAGE_URL"
        private const val ARG_RATING = "RATING"
        private const val ARG_NO_OF_BOOKINGS = "NO_OF_BOOKINGS"
        private const val ARG_TAG = "TAG"

        @JvmStatic
        fun newInstance(
            name: String,
            profileImageUrl: String = "",
            rating: Float = 0f,
            noOfBookings: String = "0",
            tag: String? = null
        ) =
            ActivityFragmentClient_ProviderProfile().apply {
                arguments = Bundle().apply {
                    name.let {  putString(ARG_NAME, name) }
                    profileImageUrl.let {  putString(ARG_PROFILE_IMAGE_URL, profileImageUrl)}
                    rating .let { putFloat(ARG_RATING, rating)}
                    noOfBookings.let { putString(ARG_NO_OF_BOOKINGS, noOfBookings)}
                    tag?.let { putString(ARG_TAG, it) }
                }
            }
    }
}
