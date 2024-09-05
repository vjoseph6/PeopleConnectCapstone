package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ServiceProviderAdapter
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.firebase.database.*

class HomeFragmentClient : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var serviceProviderAdapter: ServiceProviderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var clientInterests: List<String>
    private var email: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvInterests)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Retrieve arguments (such as email) passed to the fragment
        arguments?.let {
            email = it.getString("EMAIL")
        }

        // Proceed only if email is available
        email?.let { clientEmail ->
            databaseReference = FirebaseDatabase.getInstance().getReference("users")

            // Fetch client interests based on email, then fetch service providers
            fetchClientInterestsByEmail(clientEmail) { interests ->
                clientInterests = interests
                fetchServiceProviders() // Fetch providers only after interests are loaded
            }
        }
    }

    // Function to fetch the logged-in client's interests using email
    private fun fetchClientInterestsByEmail(email: String, onInterestsFetched: (List<String>) -> Unit) {
        // Query the database to find the user by email and retrieve their interests
        val clientReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .orderByChild("email")
            .equalTo(email)

        clientReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val interests = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.interest?.let { interests.addAll(it) } // Assuming interests are stored as a list
                }
                onInterestsFetched(interests) // Pass the interests list to the callback
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        })
    }

    // Fetch service providers and filter by client's interests
    private fun fetchServiceProviders() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serviceProviderList = mutableListOf<User>()

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user?.roles?.contains("Service Provider") == true) {
                        val providerSkills = user.skills ?: emptyList() // Assuming user.skills is a list of skills

                        // Check if the provider has at least one matching skill with the client's interests
                        if (providerSkills.any { skill -> clientInterests.contains(skill) }) {
                            serviceProviderList.add(user)
                        }
                    }
                }

                if (serviceProviderList.isNotEmpty()) {
                    serviceProviderAdapter = ServiceProviderAdapter(serviceProviderList)
                    recyclerView.adapter = serviceProviderAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        })
    }
}
