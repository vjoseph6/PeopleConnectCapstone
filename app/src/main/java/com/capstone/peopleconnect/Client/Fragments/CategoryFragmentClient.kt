package com.capstone.peopleconnect.Client.Fragments


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoryAdapter
import com.capstone.peopleconnect.Adapters.OnCategoryClickListener
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.*

class CategoryFragmentClient : Fragment(), OnCategoryClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var database: DatabaseReference
    private var categoryList: MutableList<Category> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_client, container, false)

        // Set up RecyclerView with a GridLayoutManager of 3 columns
        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        categoryAdapter = CategoryAdapter(categoryList, this)  // Pass `this` as listener
        recyclerView.adapter = categoryAdapter

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("category")

        // Retrieve categories from Firebase
        loadCategoriesFromFirebase()

        return view
    }

    // Method to load categories from Firebase
    private fun loadCategoriesFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    val name = categorySnapshot.key ?: continue
                    val image = categorySnapshot.child("image").getValue(String::class.java) ?: ""

                    // Create and add category to the list
                    val category = Category(image = image, name = name)
                    categoryList.add(category)
                }
                categoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CategoryFragmentClient", "Database error: ${error.message}")
            }
        })
    }

    // Handle category click and navigate to subcategories
    override fun onCategoryClick(category: Category) {
        // Replace current fragment with the subcategories fragment
        val subCategoryFragment = SubCategoryFragment.newInstance(category.name)
        fragmentManager?.beginTransaction()
            ?.replace(R.id.frame_layout, subCategoryFragment)
            ?.addToBackStack(null)
            ?.commit()
    }
}

