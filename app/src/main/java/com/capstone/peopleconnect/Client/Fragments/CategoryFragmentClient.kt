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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_client, container, false)

        arguments?.let {
            email = it.getString("EMAIL")
        }

        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        database = FirebaseDatabase.getInstance().getReference("category")

        fetchCategories()

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
        fetchCategories()  // Reload categories from Firebase to ensure data is refreshed
    }

    // Method to load categories from Firebase
    private fun fetchCategories() {
        // Only load categories if we are not already in the subcategories view
        if (!isInSubcategoriesView) {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryList.clear()  // Make sure to clear the list
                    for (categorySnapshot in snapshot.children) {
                        val categoryName = categorySnapshot.key ?: continue
                        val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val category = Category(name = categoryName, image = categoryImage)
                        categoryList.add(category)
                    }

                    // Re-initialize the adapter with categories only
                    categoryAdapter = CategoryAdapter(categoryList) { category ->
                        fetchSubcategories(category.name)
                    }

                    recyclerView.adapter = categoryAdapter  // Set the adapter to display categories
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }
            })
        }
    }

    // Method to load subcategories from Firebase
    private fun fetchSubcategories(categoryName: String) {
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

                    categoryAdapter = CategoryAdapter(subcategoryList) { subcategory ->
                        // Create a new instance of ClientChooseProvider using the companion object
                        val clientChooseProviderFragment = ClientChooseProvider.newInstance(
                            subCategoryName = subcategory.name,
                            email = email.toString()
                        )

                        // Navigate to the ClientChooseProvider fragment
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, clientChooseProviderFragment) // Adjust the container ID as necessary
                            .addToBackStack(null)
                            .commit()

                        Log.d("SUB CATEGORY", subcategory.name)
                        Log.d("EMAIL", email.toString())
                    }
                recyclerView.adapter = categoryAdapter
                } else {
                    // Handle empty subcategory list case if needed
                    recyclerView.adapter = null  // Clear adapter if no subcategories found
                }

            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
                // Optionally show a message to the user
            }
        })
    }

}


