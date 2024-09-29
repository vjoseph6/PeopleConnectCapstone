package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

    }

    private fun retrieveProviders() {
        // Reference to the "skills" node
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        // Fetching the data from the "skills" node
        skillsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Iterate over each child of "skills" to access dynamic keys like "-O6zJjtezJ_svfQ02VP7"
                    for (skillSetSnapshot in snapshot.children) {
                        val user = skillSetSnapshot.child("user").getValue(String::class.java)
                        val skillItemsSnapshot = skillSetSnapshot.child("skillItems")

                        if (skillItemsSnapshot.exists()) {
                            for (skillSnapshot in skillItemsSnapshot.children) {
                                val skillName = skillSnapshot.child("name").getValue(String::class.java)
                                val skillRate = skillSnapshot.child("skillRate").getValue(Int::class.java)
                                val description = skillSnapshot.child("description").getValue(String::class.java)

                                // Check if the skill name matches the selected subCategoryName
                                if (skillName != null && skillName == subCategoryName) {
                                    Log.d("SkillMatch", "Skill: $skillName, Rate: $skillRate, Description: $description, User: $user")

                                    // Pass the data to retrieve the userName from the "users" node
                                    retrieveUserName(user!!, skillName, skillRate, description)
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
    private fun retrieveUserName(userEmail: String, skillName: String?, skillRate: Int?, description: String?) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userName = userSnapshot.child("name").getValue(String::class.java)
                            val imageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)

                            // Log the retrieved data for debugging
                            Log.d("UserData", "UserName: $userName, ImageURL: $imageUrl, Skill: $skillName, Rate: $skillRate, Description: $description")

                            // Add the retrieved data to the provider list and notify the adapter
                            providerList.add(
                                ProviderData(
                                    name = skillName,
                                    skillRate = skillRate,
                                    description = description,
                                    userName = userName,
                                    imageUrl = imageUrl // Adding imageUrl to ProviderData
                                )
                            )
                        }
                        // Notify adapter after all items are added
                        providerAdapter.notifyDataSetChanged()
                    } else {
                        Log.d("UserData", "No user found with email: $userEmail")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors if necessary
                    Log.e("UserData", "Database error: ${error.message}")
                }
            })
    }
}