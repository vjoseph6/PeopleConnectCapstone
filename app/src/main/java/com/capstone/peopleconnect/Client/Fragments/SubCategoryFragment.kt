package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.SubCategoryAdapter
import com.capstone.peopleconnect.Classes.SubCategory
import com.capstone.peopleconnect.R
import com.google.firebase.database.*

class SubCategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var subCategoryAdapter: SubCategoryAdapter
    private lateinit var database: DatabaseReference
    private var subCategoryList: MutableList<SubCategory> = mutableListOf()

    private var selectedCategory: String? = null // Passed from CategoryFragmentClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the selected category passed as an argument
        selectedCategory = arguments?.getString("selectedCategory")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sub_category, container, false)

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.rvSubcategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        subCategoryAdapter = SubCategoryAdapter(subCategoryList)
        recyclerView.adapter = subCategoryAdapter

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("category").child(selectedCategory!!).child("Sub Categories")

        // Load subcategories from Firebase
        loadSubCategoriesFromFirebase()

        return view
    }

    private fun loadSubCategoriesFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subCategoryList.clear() // Clear previous data
                for (subCategorySnapshot in snapshot.children) {
                    val name = subCategorySnapshot.child("name").getValue(String::class.java) ?: continue
                    val image = subCategorySnapshot.child("image").getValue(String::class.java) ?: ""

                    // Add the retrieved subcategory to the list
                    val subCategory = SubCategory(name = name, image = image)
                    subCategoryList.add(subCategory)
                }
                subCategoryAdapter.notifyDataSetChanged() // Notify adapter to refresh the list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SubCategoryFragment", "Error fetching data: ${error.message}")
            }
        })
    }


    companion object {
        // Helper function to create a new instance of SubCategoryFragment with arguments
        @JvmStatic
        fun newInstance(selectedCategory: String) =
            SubCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString("selectedCategory", selectedCategory)
                }
            }
    }
}

