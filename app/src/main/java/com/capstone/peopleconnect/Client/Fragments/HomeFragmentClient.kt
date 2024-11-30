package com.capstone.peopleconnect.Client.Fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.auth.FirebaseAuth


class HomeFragmentClient : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var serviceProviderAdapter: ServiceProviderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var clientInterests: List<String>
    private var email: String? = null
    private var firstName: TextView? = null
    // Add at the top with other properties
    private lateinit var notificationBadge: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout and return the view
        return inflater.inflate(R.layout.fragment_home_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add after existing view initialization
        notificationBadge = view.findViewById(R.id.notificationBadge)
        setupNotificationBadge()

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

    private fun setupNotificationBadge() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(user.uid)

            notificationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unreadCount = 0
                    snapshot.children.forEach { notification ->
                        val isRead = notification.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) unreadCount++
                    }

                    activity?.runOnUiThread {
                        if (unreadCount > 0) {
                            notificationBadge.visibility = View.VISIBLE
                            notificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        } else {
                            notificationBadge.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read notifications", error.toException())
                }
            })
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

        clientReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val interests = mutableListOf<String>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let { currentUser ->
                        // Check if userPref exists and is not empty
                        if (!currentUser.userPref.isNullOrEmpty()) {
                            interests.addAll(currentUser.userPref)
                        } else {
                            // Fallback to interest if userPref is empty or null
                            currentUser.interest?.let { interestList ->
                                interests.addAll(interestList)
                            }
                        }
                        firstName!!.text = currentUser.firstName
                    }
                }
                onInterestsFetched(interests)
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
                                serviceProviderAdapter = ServiceProviderAdapter(serviceProviderList) { provider ->
                                    showSkillSelectionDialog(provider.email)
                                }
                                recyclerView.adapter = serviceProviderAdapter
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching service providers: ${error.message}")
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

    private fun showSkillSelectionDialog(providerEmail: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")
        skillsRef.orderByChild("user").equalTo(providerEmail).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val skills = mutableListOf<SkillItem>()
                for (skillSnapshot in snapshot.children) {
                    val skillItemsSnapshot = skillSnapshot.child("skillItems")
                    for (itemSnapshot in skillItemsSnapshot.children) {
                        val visible = itemSnapshot.child("visible").getValue(Boolean::class.java) ?: true // Default to true if not specified
                        if (visible) { // Only add skill if visible is true
                            val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                            val description = itemSnapshot.child("description").getValue(String::class.java) ?: ""
                            val noOfBookings = itemSnapshot.child("noOfBookings").getValue(Int::class.java) ?: 0
                            val rating = itemSnapshot.child("rating").getValue(Float::class.java) ?: 0f
                            val skillRate = itemSnapshot.child("skillRate").getValue(Int::class.java) ?: 0 // Retrieve as Int

                            val skill = SkillItem(name, description = description, noOfBookings = noOfBookings, rating = rating, skillRate = skillRate)
                            skills.add(skill)
                        }
                    }
                }

                if (skills.isNotEmpty()) {
                    fetchUserByEmail(providerEmail) { user ->
                        if (user != null) {
                            val skillNames = skills.map { it.name }.toTypedArray()
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle("Select a Service")
                            builder.setItems(skillNames) { _, which ->
                                val selectedSkill = skills[which]
                                navigateToProviderProfile(providerEmail, selectedSkill, user.name, user.profileImageUrl)
                            }
                            builder.show()
                        } else {
                            Toast.makeText(context, "Provider details not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "No skills available for this provider.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching skills: ${error.message}")
            }
        })
    }


    private fun navigateToProviderProfile(providerEmail: String, selectedSkill: SkillItem, providerName: String?, profileImageUrl: String?) {
        val providerProfileFragment = ActivityFragmentClient_ProviderProfile().apply {
            arguments = Bundle().apply {
                putString("EMAIL", providerEmail)
                putString("SERVICE_OFFERED", selectedSkill.name)
                putString("DESCRIPTION", selectedSkill.description)
                putString("NO_OF_BOOKINGS", selectedSkill.noOfBookings.toString())
                putFloat("RATING", selectedSkill.rating)
                putString("RATE", selectedSkill.skillRate.toString())
                putString("NAME", providerName)
                putString("PROFILE_IMAGE_URL", profileImageUrl)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, providerProfileFragment)
            .addToBackStack(null)
            .commit()
    }

}
