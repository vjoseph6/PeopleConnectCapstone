package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ServiceProviderAdapter
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.Classes.Skills
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.NotificationFragmentSProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class HomeFragmentClient : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var serviceProviderAdapter: ServiceProviderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var clientInterests: List<String>
    private var email: String? = null
    private var firstName: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout and return the view
        return inflater.inflate(R.layout.fragment_home_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update the date TextView
        updateDateText(view)

        // Notification icons
        val notificationIcons: LinearLayout = view.findViewById(R.id.notificationLayout)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()
        }

        // Message icons
        val messageIcons: LinearLayout = view.findViewById(R.id.messageLayout)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView = view.findViewById(R.id.rvInterests)
        recyclerView.layoutManager = LinearLayoutManager(context)
        firstName = view.findViewById(R.id.tvName)

        // Retrieve arguments (such as email) passed to the fragment
        arguments?.let {
            email = it.getString("EMAIL")
        }

        // Proceed only if email is available
        email?.let { clientEmail ->
            databaseReference = FirebaseDatabase.getInstance().getReference("users")

            // Fetch client interests based on email
            fetchClientInterestsByEmail(clientEmail) { interests ->
                clientInterests = interests

                // Fetch service providers based on skills collection
                fetchServiceProvidersBySkills(clientInterests, clientEmail)
            }
        }
    }

    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())

        // Find the TextView and set the formatted date
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        tvDate.text = currentDate // Set the formatted date to the TextView
    }


    private fun fetchClientInterestsByEmail(email: String, onInterestsFetched: (List<String>) -> Unit) {
        val clientReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .orderByChild("email")
            .equalTo(email)

        clientReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val interests = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.interest?.let { interestList ->
                        interests.addAll(interestList)
                    }
                    firstName!!.text = user?.firstName.toString()

                }
                onInterestsFetched(interests) // Pass the interests list to the callback
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragmentClient", "Error fetching client interests: ${error.message}")
            }
        })
    }

    private fun fetchServiceProvidersBySkills(clientInterests: List<String>, clientEmail: String) {
        val skillsReference = FirebaseDatabase.getInstance().getReference("skills")

        skillsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(skillSnapshot: DataSnapshot) {
                val serviceProviderList = mutableListOf<User>()

                for (skillUserSnapshot in skillSnapshot.children) {
                    val userEmail = skillUserSnapshot.child("user").getValue(String::class.java) ?: continue
                    fetchUserByEmail(userEmail) { user ->
                        if (user?.roles?.contains("Service Provider") == true) {
                            val skillItemsSnapshot = skillUserSnapshot.child("skillItems")
                            val visibleSkills = skillItemsSnapshot.children.filter { skillItem ->
                                val skill = skillItem.getValue(SkillItem::class.java)
                                skill != null && skill.visible && clientInterests.contains(skill.name)
                            }

                            if (visibleSkills.isNotEmpty() && user.email != clientEmail) {
                                serviceProviderList.add(user)
                            }

                            // Check if fragment is still attached before setting the adapter
                            context?.let {
                                recyclerView.layoutManager = GridLayoutManager(it, 2)
                                serviceProviderAdapter = ServiceProviderAdapter(serviceProviderList)
                                recyclerView.adapter = serviceProviderAdapter
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        })
    }

    private fun fetchUserByEmail(email: String, onUserFetched: (User?) -> Unit) {
        val usersReference = FirebaseDatabase.getInstance()
            .getReference("users")
            .orderByChild("email")
            .equalTo(email)

        usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    onUserFetched(user)
                    return // Assuming only one user per email, so we return after the first match
                }
                onUserFetched(null) // No user found
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        })
    }

}
