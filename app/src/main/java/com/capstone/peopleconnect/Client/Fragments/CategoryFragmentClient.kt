package com.capstone.peopleconnect.Client.Fragments



import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoryAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.Helper.ClickData
import com.capstone.peopleconnect.Helper.RetrofitInstance
import com.capstone.peopleconnect.R
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CategoryFragmentClient : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var database: DatabaseReference
    private var email: String? = null
    private var categoryList: MutableList<Category> = mutableListOf()
    private var isInSubcategoriesView = false  // Flag to track the state

    // To handle double back press
    private var backPressedOnce = false
    private val backPressHandler = Handler()

    //this is for the variables to be passed on each screens
    private var bookDay: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private var rating: String? = null
    private var serviceType: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_client, container, false)

        Log.d("CategoryFragmentClient", "Arguments: ${arguments?.getString("serviceType")}")

        arguments?.let {
            email = it.getString("EMAIL")
            bookDay = it.getString("bookDay")
            startTime = it.getString("startTime")
            endTime = it.getString("endTime")
            rating = it.getString("rating")
            serviceType = it.getString("serviceType")

            Log.d("CategoryFragmentClient", "Email: $email, BookDay: $bookDay, StartTime: $startTime, EndTime: $endTime, Rating: $rating, ServiceType: $serviceType")
        } ?: Log.d("CategoryFragmentClient", "Arguments are null")



        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        database = FirebaseDatabase.getInstance().getReference("category")

        fetchCategoriesAndCheckSubcategory(serviceType)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleFragmentBackPress()
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        updateDateText(view)

        // Notification icons
        val notificationIcons: LinearLayout = view.findViewById(R.id.notificationLayout)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()

        }

        // Message icons
        val messageIcons: LinearLayout = view.findViewById(R.id.messageLayout)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()

        }

        val searchCategory: EditText = view.findViewById(R.id.searchCategory)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        database = FirebaseDatabase.getInstance().getReference("category")

        // Fetch categories and set up filtering
        fetchCategoriesAndCheckSubcategory(serviceType)

        // Set up TextWatcher for search functionality
        searchCategory.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCategories(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleFragmentBackPress()
            }
        })
        


    }

    private fun filterCategories(query: String) {
        val filteredSubcategories = mutableListOf<Category>()

        // Loop through each category to filter out subcategories
        categoryList.forEach { category ->
            val subcategoryReference = database.child(category.name).child("Sub Categories")

            subcategoryReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (subcategorySnapshot in snapshot.children) {
                        val subcategoryName = subcategorySnapshot.child("name").getValue(String::class.java) ?: continue
                        val subcategoryImage = subcategorySnapshot.child("image").getValue(String::class.java) ?: ""

                        // Check if the subcategory matches the query
                        if (subcategoryName.contains(query, ignoreCase = true)) {
                            val subcategory = Category(name = subcategoryName, image = subcategoryImage)
                            filteredSubcategories.add(subcategory)
                        }
                    }

                    // After collecting all matching subcategories, set up the adapter with the updated list
                    categoryAdapter = CategoryAdapter(filteredSubcategories) { subcategory ->
                        navigateToChooseProviderFragment(subcategory)  // Keep the click listener for navigation
                    }
                    recyclerView.adapter = categoryAdapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Database error: ${error.message}")
                }
            })
        }
    }




    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())

        // Find the TextView and set the formatted date
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        tvDate.text = currentDate // Set the formatted date to the TextView
    }

    fun handleFragmentBackPress() {
        if (isInSubcategoriesView) {
            displayCategories()  // Go back to the category list
        } else {
            if (backPressedOnce) {
                requireActivity().finishAffinity()  // Exit the app
            } else {
                backPressedOnce = true
                Toast.makeText(context, "Press again to exit the application", Toast.LENGTH_SHORT).show()
                backPressHandler.postDelayed({ backPressedOnce = false }, 2000)  // Reset after 2 seconds
            }
        }
    }

    // Function to handle category click
    private fun onCategoryClick(categoryName: String, userId: String) {
        // Create ClickData object
        val clickData = ClickData(user_id = userId, clicked_category = categoryName)

        // Make POST request to update clicks
        RetrofitInstance.api.updateClicks(clickData).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(
                call: Call<Map<String, String>>,
                response: Response<Map<String, String>>
            ) {
                if (response.isSuccessful) {
                    // If POST was successful, now get recommendations
                    fetchRecommendations(userId)
                    Log.d("USER ID IS:", "$userId")
                } else {
                    Log.e("API Error", "Failed to update clicks")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("API Error", "Network error: ${t.message}")
            }
        })
    }

    // Function to fetch recommendations
    private fun fetchRecommendations(userId: String) {
        Log.d("USER ID recommend:", "$userId")
        RetrofitInstance.api.getRecommendations(userId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val recommendationsMap = responseBody["recommendations"] as? Map<String, List<String>>
                        val recentClicksList = responseBody["recent_clicks"] as? List<String>

                        // Extract only the recommendation details and recent clicks
                        val userPref = recommendationsMap?.values?.flatMap { it } ?: listOf()  // Flatten all subcategories
                        val userClicks = recentClicksList ?: listOf()

                        Log.d("UserPreferences", "User Preferences: $userPref")
                        Log.d("UserClicks", "User Clicks: $userClicks")

                        saveUserDataToFirebase(userId, userPref, userClicks)


                    }
                } else {
                    Log.e("API Error", "Failed to get recommendations")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("API Error", "Network error: ${t.message}")
            }
        })
    }

    // Function to save user data in Firebase Realtime Database
    private fun saveUserDataToFirebase(userId: String, userPref: List<String>, userClicks: List<String>) {
        // Query the database for a user where the email matches the userId
        FirebaseDatabase.getInstance().getReference("users").orderByChild("email").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // If a user with the matching email is found, update userPref and userClicks
                        for (userSnapshot in snapshot.children) {
                            // Only update userPref and userClicks fields
                            userSnapshot.ref.child("userPref").setValue(userPref)
                            userSnapshot.ref.child("userClicks").setValue(userClicks)
                                .addOnSuccessListener {
                                    Log.d("Firebase", "User data updated successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firebase", "Failed to update user data: ${e.message}")
                                }
                        }
                    } else {
                        // If no matching email is found, log or handle the case
                        Log.e("Firebase", "No user found with the specified email")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Database error: ${error.message}")
                }
            })
    }


    // Method to display categories again
    private fun displayCategories() {
        isInSubcategoriesView = false  // Reset the flag
        fetchCategoriesAndCheckSubcategory(serviceType)  // Reload categories from Firebase to ensure data is refreshed
    }

    // Method to load categories from Firebase
    // Fused Method to fetch categories and check for matching subcategories
    private fun fetchCategoriesAndCheckSubcategory(serviceType: String?) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()  // Clear the list first

                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: continue
                    val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                    val category = Category(name = categoryName, image = categoryImage)
                    categoryList.add(category)

                    // Check for subcategories in this category
                    val subcategoryReference = categorySnapshot.child("Sub Categories")
                    val subcategoryList = mutableListOf<Category>()

                    for (subcategorySnapshot in subcategoryReference.children) {
                        val subcategoryName = subcategorySnapshot.child("name").getValue(String::class.java) ?: continue
                        val subcategoryImage = subcategorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val subcategory = Category(name = subcategoryName, image = subcategoryImage)
                        subcategoryList.add(subcategory)

                        // Check if the subcategory matches the serviceType
                        if (subcategoryName.equals(serviceType, ignoreCase = true)) {
                            // Match found for serviceType, simulate clicking the category and subcategory
                            isInSubcategoriesView = true
                            navigateToChooseProviderFragment(subcategory)
                            return  // Exit the loop once we find a match
                        }
                    }

                    // If no match found, continue setting up categories normally
                    categoryAdapter = CategoryAdapter(categoryList) { category ->
                        // Fetch subcategories when category is clicked
                        fetchSubcategories(category.name, serviceType)
                        onCategoryClick(category.name, email.toString())
                    }
                    recyclerView.adapter = categoryAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    // Method to load subcategories when a category is clicked
    private fun fetchSubcategories(categoryName: String, serviceType: String?) {
        val subcategoryReference = database.child(categoryName).child("Sub Categories")

        subcategoryReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val subcategoryList = mutableListOf<Category>()
                for (subcategorySnapshot in snapshot.children) {
                    val subcategoryName = subcategorySnapshot.child("name").getValue(String::class.java) ?: continue
                    val subcategoryImage = subcategorySnapshot.child("image").getValue(String::class.java) ?: ""
                    val subcategory = Category(name = subcategoryName, image = subcategoryImage)
                    subcategoryList.add(subcategory)
                }

                if (subcategoryList.isNotEmpty()) {
                    isInSubcategoriesView = true  // Set the flag when subcategories are displayed

                    // Set up the adapter for subcategories
                    categoryAdapter = CategoryAdapter(subcategoryList) { subcategory ->
                        navigateToChooseProviderFragment(subcategory)  // Navigate on click
                    }
                    recyclerView.adapter = categoryAdapter
                } else {
                    // Handle empty subcategory list if needed
                    recyclerView.adapter = null  // Clear adapter if no subcategories found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    // Method to handle navigation to ClientChooseProvider
    private fun navigateToChooseProviderFragment(subcategory: Category) {
        val clientChooseProviderFragment = ClientChooseProvider.newInstance(
            subCategoryName = subcategory.name,
            email = email.toString(),
            bookDay = bookDay.toString(),
            startTime = startTime.toString(),
            endTime = endTime.toString()
        )

        // Navigate to the ClientChooseProvider fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, clientChooseProviderFragment)  // Adjust the container ID as necessary
            .addToBackStack(null)
            .commit()

        Log.d("SUB CATEGORY", subcategory.name)
        Log.d("EMAIL", email.toString())
    }

}


