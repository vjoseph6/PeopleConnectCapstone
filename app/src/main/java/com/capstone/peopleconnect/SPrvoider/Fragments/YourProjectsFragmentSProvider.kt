package com.capstone.peopleconnect.SPrvoider.Fragments

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.CategoryAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class YourProjectsFragmentSProvider : Fragment() {

    private var email: String? = null
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_your_projects_s_provider, container, false) // Use your layout file here
        recyclerView = view.findViewById(R.id.skills)
        categoryAdapter = CategoryAdapter(mutableListOf()) { category ->
            val skillPostFragment = SkillsPostFragmentSProvider.newInstance(
                email = email.toString(),
                categoryName = category.name
            )


            // Navigate to the ClientChooseProvider fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, skillPostFragment) // Adjust the container ID as necessary
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = categoryAdapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email?.let {
            fetchSkills(it) // Fetch skills as soon as the view is created
        }
    }



    private fun fetchSkills(email: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.orderByChild("user").equalTo(email).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d(ContentValues.TAG, "Skills data exists for user: $email")
                    val categories = mutableListOf<Category>() // Prepare a new list for categories
                    var totalItems = 0
                    var fetchedItems = 0

                    for (skillSnapshot in snapshot.children) {
                        totalItems += skillSnapshot.child("skillItems").childrenCount.toInt()
                        val skillItemsSnapshot = skillSnapshot.child("skillItems")
                        for (itemSnapshot in skillItemsSnapshot.children) {
                            val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                            Log.d(ContentValues.TAG, "Retrieved skill name: $name")

                            // Fetch the image URL using the name
                            fetchImage(name) { imageUrl ->
                                // Create a new Category object and add it to the list
                                val category = Category(name = name, image = imageUrl)
                                categories.add(category)
                                Log.d(ContentValues.TAG, "Fetched image URL for $name: $imageUrl")

                                fetchedItems++
                                // Update the adapter only after all items have been fetched
                                if (fetchedItems == totalItems) {
                                    categoryAdapter.updateCategories(categories)
                                }
                            }
                        }
                    }
                } else {
                    Log.d(ContentValues.TAG, "No skills data found for user: $email")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Database error: ${error.message}")
            }
        })
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
                            Log.d(ContentValues.TAG, "Match Found: $subName, Image: $image")
                            callback(image ?: "")
                            return // Exit loop once a match is found
                        }
                    }
                }
                // If no match found, return an empty string
                callback("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Database error: ${error.message}")
                callback("") // Pass an empty string if there's an error
            }
        })
    }
    companion object {

        @JvmStatic
        fun newInstance(email:String?) = YourProjectsFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}