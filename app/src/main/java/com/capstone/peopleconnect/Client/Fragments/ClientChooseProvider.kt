package com.capstone.peopleconnect.Client.Fragments

import android.content.Intent
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
    private lateinit var skillName: String
    private lateinit var providerAdapter: ProviderAdapter
    private val providerList = mutableListOf<ProviderData>()
    private var subCategoryName: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return inflater.inflate(R.layout.fragment_client_choose_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        retrieveProviders() // Call to fetch providers should happen here

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
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        providerAdapter = ProviderAdapter(providerList) { provider ->

            // Handle provider item clicks
            val fragment = ActivityFragmentClient_ProviderProfile().apply {
                arguments = Bundle().apply {
                    putString("NAME", provider.userName)
                    putString("PROFILE_IMAGE_URL", provider.imageUrl)
                    putFloat("RATING", provider.rating ?: 0f)
                    putInt("NO_OF_BOOKINGS", provider.noOfBookings ?: 0)
                    putString("DESCRIPTION", provider.description)
                    putString("RATE", (provider.skillRate ?: 0).toString())
                    putString("SERVICE_OFFERED", subCategoryName)
                    Log.d("SKILL NAME" , subCategoryName.toString())
                }
            }


            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = providerAdapter

    }

    private fun retrieveProviders() {
        // Clear the provider list to avoid duplication
        providerList.clear() // Clear existing data
        providerAdapter.notifyDataSetChanged() // Notify the adapter about the cleared list

        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.addValueEventListener(object : ValueEventListener {
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
                                skillName = skillSnapshot.child("name").getValue(String::class.java).toString()
                                val skillRate = skillSnapshot.child("skillRate").getValue(Int::class.java)
                                val description = skillSnapshot.child("description").getValue(String::class.java)
                                val rating = skillSnapshot.child("rating").getValue(Float::class.java)
                                val isVisible = skillSnapshot.child("visible").getValue(Boolean::class.java) ?: false

                                if (skillName == subCategoryName && isVisible) {
                                    user?.let { retrieveUserName(it, skillName, skillRate, description, rating) }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SkillData", "Database error: ${error.message}")
            }
        })
    }

    private fun retrieveUserName(userEmail: String, skillName: String?, skillRate: Int?, description: String?, rating: Float?) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.orderByChild("email").equalTo(userEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userName = userSnapshot.child("name").getValue(String::class.java)
                            val imageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                            val noOfBookings = userSnapshot.child("noOfBookings").getValue(Int::class.java) ?: 0

                            Log.d("UserData", "UserName: $userName, ImageURL: $imageUrl, Skill: $skillName, Rate: $skillRate, Description: $description, Rating: $rating, NoOfBookings: $noOfBookings")

                            if (userEmail != email) {
                                providerList.add(
                                    ProviderData(
                                        name = skillName,
                                        skillRate = skillRate,
                                        description = description,
                                        userName = userName,
                                        imageUrl = imageUrl,
                                        rating = rating,
                                        noOfBookings = noOfBookings // Include noOfBookings
                                    )
                                )
                            }
                        }
                        providerAdapter.notifyDataSetChanged()
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