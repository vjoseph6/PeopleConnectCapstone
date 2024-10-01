package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ProviderAdapter
import com.capstone.peopleconnect.Classes.ProviderData
import com.capstone.peopleconnect.R
import com.google.firebase.database.*

class ClientChooseProvider : Fragment() {
    private lateinit var providerAdapter: ProviderAdapter
    private val providerList = mutableListOf<ProviderData>()
    private var subCategoryName: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments passed to the fragment
        arguments?.let {
            subCategoryName = it.getString("SUBCATEGORY_NAME")
            email = it.getString("EMAIL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_client_choose_provider, container, false)

        setupRecyclerView(view)
        retrieveProviders()

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackClient)
        backBtn.setOnClickListener {
            val categoryFragment = CategoryFragmentClient().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("FRAGMENT_TO_LOAD", "CategoryFragmentClient")
                }
            }

            // Perform the fragment transaction with the tag and email
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, categoryFragment, "CategoryFragmentClient")
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        providerAdapter = ProviderAdapter(providerList)
        recyclerView.adapter = providerAdapter
    }

    private fun retrieveProviders() {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (skillSetSnapshot in snapshot.children) {
                        val user = skillSetSnapshot.child("user").getValue(String::class.java)

                        // Skip if the user matches the current user's email
                        if (user == email) {
                            continue
                        }

                        val skillItemsSnapshot = skillSetSnapshot.child("skillItems")

                        if (skillItemsSnapshot.exists()) {
                            for (skillSnapshot in skillItemsSnapshot.children) {
                                val skillName = skillSnapshot.child("name").getValue(String::class.java)
                                val skillRate = skillSnapshot.child("skillRate").getValue(Int::class.java)
                                val description = skillSnapshot.child("description").getValue(String::class.java)
                                val rating = skillSnapshot.child("rating").getValue(Float::class.java)
                                val isVisible = skillSnapshot.child("visible").getValue(Boolean::class.java) ?: false

                                // Check if the skill name matches and the skill is visible
                                if (skillName == subCategoryName && isVisible) {
                                    Log.d("SkillMatch", "Skill: $skillName, Rate: $skillRate, Description: $description, Rating: $rating, User: $user")
                                    user?.let { retrieveUserName(it, skillName, skillRate, description, rating) }
                                } else if (!isVisible) {
                                    Log.d("SkillVisibility", "Skipping skill: $skillName as it is not visible.")
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

    private fun retrieveUserName(userEmail: String, skillName: String?, skillRate: Int?, description: String?, rating: Float?) {
        // Function to retrieve userName from the "users" node based on userEmail
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userName = userSnapshot.child("name").getValue(String::class.java)
                            val imageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                            Log.d("UserData", "UserName: $userName, ImageURL: $imageUrl, Skill: $skillName, Rate: $skillRate, Description: $description, Rating: $rating")

                            // Check if the userEmail matches the email retrieved from the intent
                            if (userEmail != email) { // use email from the fragment arguments
                                providerList.add(
                                    ProviderData(
                                        name = skillName,
                                        skillRate = skillRate,
                                        description = description,
                                        userName = userName,
                                        imageUrl = imageUrl,
                                        rating = rating
                                    )
                                )
                            } else {
                                Log.d("UserData", "Skipping user with email: $userEmail, as it matches the intent email: $email")
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

    companion object {
        fun newInstance(subCategoryName: String, email: String) =
            ClientChooseProvider().apply {
                arguments = Bundle().apply {
                    putString("SUBCATEGORY_NAME", subCategoryName)
                    putString("EMAIL", email)
                }
            }
    }
}