package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ProviderAdapter
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ActivityFragmentClient_Recommended : Fragment() {
    private lateinit var database: DatabaseReference
    private lateinit var providerAdapter: ProviderAdapter
    private lateinit var subcategoryName: String
    private lateinit var email: String
    private lateinit var providerRecyclerView: RecyclerView // Declare RecyclerView as a member variable
    private val providerList = mutableListOf<String>() // List for provider names

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subcategoryName = it.getString("SubCategoryName").orEmpty()
            email = it.getString("EMAIL").orEmpty()
        }

        database = FirebaseDatabase.getInstance().getReference("skills")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_client__recommended, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        providerRecyclerView = view.findViewById(R.id.recyclerView) // Ensure this matches your XML ID
        providerRecyclerView.layoutManager = LinearLayoutManager(context)
        providerAdapter = ProviderAdapter(emptyList())
        providerRecyclerView.adapter = providerAdapter

        fetchProviderNames()
    }

    private fun fetchProviderNames() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val providerNames = mutableListOf<String>()
                for (skillSnapshot in snapshot.children) {
                    val skillName = skillSnapshot.child("name").getValue(String::class.java)
                    val skillUser = skillSnapshot.child("user").getValue(String::class.java)

                    // Only add the provider if the skill matches the subcategory and doesn't match the current user's email
                    if (skillName == subcategoryName && skillUser != email) {
                        val providerName = skillUser.orEmpty() // Assuming the user's email is being used as the name placeholder for now
                        providerNames.add(providerName)
                    }
                }

                // Update adapter with provider names
                providerRecyclerView.adapter = providerAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }
}