package com.capstone.peopleconnect.Client.Fragments



import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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
                Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show()
                backPressHandler.postDelayed({ backPressedOnce = false }, 2000)  // Reset after 2 seconds
            }
        }
    }

    // Method to display categories again
    private fun displayCategories() {
        isInSubcategoriesView = false  // Reset the flag
        categoryAdapter.updateCategories(categoryList)  // Update the adapter with the original categories
    }

    // Method to load categories from Firebase
    private fun fetchCategories() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: continue
                    val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                    val category = Category(name = categoryName, image = categoryImage)
                    categoryList.add(category)
                }

                // Initialize the adapter with categories and category click listener
                categoryAdapter = CategoryAdapter(categoryList) { category ->
                    // Fetch and display subcategories for the selected category
                    fetchSubcategories(category.name)
                }

                recyclerView.adapter = categoryAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    // Method to load subcategories from Firebase
    // Method to load subcategories from Firebase
    private fun fetchSubcategories(categoryName: String) {
        val subcategoryReference = database.child(categoryName).child("Sub Categories")

        subcategoryReference.addListenerForSingleValueEvent(object : ValueEventListener {
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
                    categoryAdapter.updateCategories(subcategoryList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }
}

