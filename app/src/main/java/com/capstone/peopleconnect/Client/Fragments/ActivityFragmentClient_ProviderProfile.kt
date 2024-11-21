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
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso


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
        val skillRate = arguments?.getString("RATE") ?: ""
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

        return view
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
                            Log.d(TAG, "Retrieved email: $email")
                            if (email != null) {
                                fetchSkills(email)  // Call fetchSkills with the user's email
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

        skillsRef.orderByChild("user").equalTo(email).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Skills data exists for user: $email")
                    val categories = mutableListOf<Category>() // Prepare a new list for categories
                    var totalItems = 0
                    var fetchedItems = 0

                    for (skillSnapshot in snapshot.children) {
                        totalItems += skillSnapshot.child("skillItems").childrenCount.toInt()
                        val skillItemsSnapshot = skillSnapshot.child("skillItems")
                        for (itemSnapshot in skillItemsSnapshot.children) {
                            val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                            Log.d(TAG, "Retrieved skill name: $name")

                            // Fetch the image URL using the name
                            fetchImage(name) { imageUrl ->
                                // Create a new Category object and add it to the list
                                val category = Category(name = name, image = imageUrl)
                                categories.add(category)
                                Log.d(TAG, "Fetched image URL for $name: $imageUrl")

                                fetchedItems++
                                // Update the adapter only after all items have been fetched
                                if (fetchedItems == totalItems) {
                                    servicesAdapter.updateServices(categories)
                                    servicesAdapter.updateSelectedService(serviceOffered)
                                }
                            }
                        }
                    }
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

        @JvmStatic
        fun newInstance(
            name: String,
            profileImageUrl: String,
            rating: Float,
            noOfBookings: String
        ) =
            ActivityFragmentClient_ProviderProfile().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putString(ARG_PROFILE_IMAGE_URL, profileImageUrl)
                    putFloat(ARG_RATING, rating)
                    putString(ARG_NO_OF_BOOKINGS, noOfBookings)
                }
            }
    }
}
