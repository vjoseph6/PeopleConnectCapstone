package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import com.capstone.peopleconnect.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ProviderAdapter
import com.capstone.peopleconnect.Classes.ProviderData
import com.google.firebase.database.*

class ActivityClientSelectProvider : AppCompatActivity() {

    private lateinit var providerAdapter: ProviderAdapter
    private val providerList = mutableListOf<ProviderData>()
    private var subCategoryName: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_select_provider)

        // Get the passed extras
        subCategoryName = intent.getStringExtra("SUBCATEGORY_NAME")
        email = intent.getStringExtra("EMAIL")

        Log.d("Data Retrieved", "The sub is $subCategoryName and email is $email")

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        providerAdapter = ProviderAdapter(providerList)
        recyclerView.adapter = providerAdapter

        // Retrieve providers
        retrieveProviders()

        val backBtn = findViewById<ImageButton>(R.id.btnBackClient)
        backBtn.setOnClickListener { onBackPressed() }

    }

    private fun retrieveProviders() {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (skillSetSnapshot in snapshot.children) {
                        val user = skillSetSnapshot.child("user").getValue(String::class.java)
                        val skillItemsSnapshot = skillSetSnapshot.child("skillItems")

                        if (skillItemsSnapshot.exists()) {
                            for (skillSnapshot in skillItemsSnapshot.children) {
                                val skillName = skillSnapshot.child("name").getValue(String::class.java)
                                val skillRate = skillSnapshot.child("skillRate").getValue(Int::class.java)
                                val description = skillSnapshot.child("description").getValue(String::class.java)
                                val rating = skillSnapshot.child("rating").getValue(Float::class.java) // Fetch rating from Firebase

                                // Check if the skill name matches the selected subCategoryName
                                if (skillName != null && skillName == subCategoryName) {
                                    Log.d("SkillMatch", "Skill: $skillName, Rate: $skillRate, Description: $description, Rating: $rating, User: $user")

                                    // Pass the data, including the rating, to retrieve the userName from the "users" node
                                    retrieveUserName(user!!, skillName, skillRate, description, rating, email) // Pass the email
                                }
                            }
                        } else {
                            Log.d("SkillData", "No skill items found under skill set: ${skillSetSnapshot.key}")
                        }
                    }
                } else {
                    Log.d("SkillData", "No skill sets found in the 'skills' node.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SkillData", "Database error: ${error.message}")
            }
        })
    }

    // Function to retrieve userName from the "users" node based on userEmail
    private fun retrieveUserName(userEmail: String, skillName: String?, skillRate: Int?, description: String?, rating: Float?, intentEmail: String?) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userName = userSnapshot.child("name").getValue(String::class.java)
                            val imageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)

                            // Check if the userEmail matches the email retrieved from the intent
                            if (userEmail != intentEmail) {
                                // Log the retrieved data for debugging
                                Log.d("UserData", "UserName: $userName, ImageURL: $imageUrl, Skill: $skillName, Rate: $skillRate, Description: $description, Rating: $rating")

                                // Pass the skillName as the provider category name
                                providerList.add(
                                    ProviderData(
                                        name = skillName, // This represents the provider category
                                        skillRate = skillRate,
                                        description = description,
                                        userName = userName,
                                        imageUrl = imageUrl,
                                        rating = rating
                                    )
                                )
                            } else {
                                Log.d("UserData", "Skipping user with email: $userEmail, as it matches the intent email: $intentEmail")
                            }
                        }
                        providerAdapter.notifyDataSetChanged()
                    } else {
                        Log.d("UserData", "No user found with email: $userEmail")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserData", "Database error: ${error.message}")
                }
            })
    }

}