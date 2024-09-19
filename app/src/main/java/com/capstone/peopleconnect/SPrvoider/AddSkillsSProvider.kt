package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.AddSkillsCategoryAdapter
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.firebase.database.*
class AddSkillsSProvider : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: AddSkillsCategoryAdapter
    private lateinit var databaseReference: DatabaseReference
    private var categories = mutableListOf<Category>()
    private var isInSubcategoriesView = false  // Track if we're viewing subcategories
    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_skills_sprovider)

        // Email of the current user
        email = intent.getStringExtra("EMAIL").toString()

        recyclerView = findViewById(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Firebase reference to the 'category' collection
        databaseReference = FirebaseDatabase.getInstance().getReference("category")

        // Fetch all categories from Firebase
        fetchCategories()

        // Handle back button
        val backButton: ImageButton = findViewById(R.id.btnBackSProviderSKills)
        backButton.setOnClickListener {
            if (isInSubcategoriesView) {

                fetchCategories()
                categoryAdapter.updateCategories(categories)
                recyclerView.adapter = categoryAdapter
                isInSubcategoriesView = false
                findViewById<TextView>(R.id.popularText).text = "Popular Skills"
            } else {
                onBackPressed()
            }
        }
    }

    private fun fetchCategories() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: continue
                    val category = Category(name = categoryName)
                    categories.add(category)
                }

                // Initialize the adapter with categories and category click listener
                categoryAdapter = AddSkillsCategoryAdapter(categories) { category ->
                    fetchSubcategories(category.name)
                }

                // Set the adapter for the categories list
                recyclerView.adapter = categoryAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }

    private fun fetchSubcategories(categoryName: String) {
        val subcategoryReference = databaseReference.child(categoryName).child("Sub Categories")

        subcategoryReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val subcategories = mutableListOf<Category>()
                for (subcategorySnapshot in snapshot.children) {
                    val subcategoryName = subcategorySnapshot.child("name").getValue(String::class.java) ?: continue
                    val subcategory = Category(name = subcategoryName)
                    subcategories.add(subcategory)
                }

                if (subcategories.isNotEmpty()) {
                    // Update to subcategory view
                    isInSubcategoriesView = true
                    findViewById<TextView>(R.id.popularText).text = categoryName

                    // Setup subcategory adapter with subcategory click listener to go to next activity
                    categoryAdapter = AddSkillsCategoryAdapter(subcategories) { subcategory ->
                        val intent = Intent(this@AddSkillsSProvider, AddSkillsProviderRate::class.java)
                        intent.putExtra("SUBCATEGORY_NAME", subcategory.name)
                        intent.putExtra("EMAIL", email)
                        startActivity(intent)
                    }

                    // Set the adapter for the subcategories
                    recyclerView.adapter = categoryAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }
}