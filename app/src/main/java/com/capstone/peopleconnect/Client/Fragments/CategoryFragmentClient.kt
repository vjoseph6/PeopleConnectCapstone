package com.capstone.peopleconnect.Client.Fragments



import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoryAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.*

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

    // Method to display categories again
    private fun displayCategories() {
        isInSubcategoriesView = false  // Reset the flag
        fetchCategoriesAndCheckSubcategory(serviceType)  // Reload categories from Firebase to ensure data is refreshed
    }

    // Method to load categories from Firebase
    // Fused Method to fetch categories and check for matching subcategories
    private fun fetchCategoriesAndCheckSubcategory(serviceType: String?) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
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
                            isInSubcategoriesView = true  // Set the flag for subcategories view
                            navigateToChooseProviderFragment(subcategory)  // Navigate to providers
                            return  // Exit the loop once we find a match
                        }
                    }

                    // If no match found, continue setting up categories normally
                    categoryAdapter = CategoryAdapter(categoryList) { category ->
                        // Fetch subcategories when category is clicked
                        fetchSubcategories(category.name, serviceType)
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


