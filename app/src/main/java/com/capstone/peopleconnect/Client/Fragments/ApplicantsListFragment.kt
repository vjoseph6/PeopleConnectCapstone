package com.capstone.peopleconnect.Client.Fragments

import ApplicantsAdapter
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.PostApplication
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.Glide
import android.util.Log
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth

class   ApplicantsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplicantsAdapter
    private lateinit var database: DatabaseReference
    private lateinit var emptyView: View
    private var postId: String? = null
    private var serviceOffered: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        arguments?.let {
            postId = it.getString("POST_ID")
            serviceOffered = it.getString("SERVICE_OFFERED")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_applicants_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.rvApplicants)
        emptyView = view.findViewById(R.id.emptyView)

        // Setup back button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Load applicants
        loadApplicants()

        Glide.with(this)
            .asGif()
            .load(R.drawable.nothing) // Make sure to add your GIF to res/raw folder
            .into(view.findViewById(R.id.emptyGif))
    }

    private fun setupRecyclerView() {
        adapter = ApplicantsAdapter(
            onActionClick = { application, isAccept ->
                if (isAccept) {
                    handleAccept(application)
                } else {
                    handleReject(application)
                }
            },
            fetchUserDetails = { providerEmail, callback ->
                database.child("skills")
                    .orderByChild("user")
                    .equalTo(providerEmail)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (skillSnapshot in snapshot.children) {
                                val skillItems = skillSnapshot.child("skillItems")
                                for (itemSnapshot in skillItems.children) {
                                    // Check if the skill matches the service offered from fragment arguments
                                    val skillName = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                                    if (skillName.equals(serviceOffered, ignoreCase = true)) {
                                        // Fetch user details
                                        database.child("users")
                                            .orderByChild("email")
                                            .equalTo(providerEmail)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                                    for (user in userSnapshot.children) {
                                                        val name = user.child("name").getValue(String::class.java) ?: ""
                                                        val profileImageUrl = user.child("profileImageUrl").getValue(String::class.java) ?: ""

                                                        // Extract skill details
                                                        val description = itemSnapshot.child("description").getValue(String::class.java) ?: ""
                                                        val skillRate = itemSnapshot.child("skillRate").getValue(Double::class.java) ?: 0.0
                                                        val rating = itemSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
                                                        val skillName = itemSnapshot.child("name").getValue(String::class.java) ?: ""

                                                        // Invoke callback with all details
                                                        callback(skillName,name, profileImageUrl, description, skillRate, rating)
                                                        return
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    Toast.makeText(context, "Failed to load user details", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                        return
                                    }
                                }
                            }
                            // If no matching skill found
                            Toast.makeText(context, "No matching skill found", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, "Failed to load skills", Toast.LENGTH_SHORT).show()
                        }
                    })
            },
            onApplicantClick = { providerName, tag ->
                navigateToProviderProfile(providerName, tag)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun navigateToProviderProfile(providerName: String, tag: String? = null) {
        val profileFragment = if (tag != null) {
            ActivityFragmentClient_ProviderProfile.newInstance(providerName, tag = tag)
        } else {
            ActivityFragmentClient_ProviderProfile.newInstance(providerName)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, profileFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadApplicants() {
        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val applicants = mutableListOf<PostApplication>()
                    for (applicationSnapshot in snapshot.children) {
                        val application = applicationSnapshot.getValue(PostApplication::class.java)
                        if (application?.status == "Pending" || application?.status == "Accepted") {
                            applicants.add(application!!)
                        }
                    }
                    updateUI(applicants)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load applicants", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(applicants: List<PostApplication>) {
        if (applicants.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.setApplications(applicants)
        }
    }

    private fun handleAccept(application: PostApplication) {
        // Create the custom dialog
        val dialogView = LayoutInflater.from(context).inflate(R.layout.client_dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        dialog.show()

        // Find views in the custom layout
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvLogoutTitle)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnLogout)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        // Customize the dialog for acceptance
        tvTitle.text = "Do you want to confirm application?"
        btnConfirm.text = "Accept"

        // Set click listeners
        btnConfirm.setOnClickListener {
            database.child("post_applicants")
                .orderByChild("postId")
                .equalTo(postId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (applicationSnapshot in snapshot.children) {
                            val currentApp = applicationSnapshot.getValue(PostApplication::class.java)
                            if (currentApp?.providerEmail == application.providerEmail) {
                                applicationSnapshot.ref.child("status").setValue("Accepted")

                                // Send notification to service provider
                                sendAcceptanceNotification(application.providerEmail)

                                Toast.makeText(context, "Application accepted", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }
                        dialog.dismiss()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to accept application", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                })
        }

        // Cancel listener
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun handleReject(application: PostApplication) {
        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (applicationSnapshot in snapshot.children) {
                        val currentApp = applicationSnapshot.getValue(PostApplication::class.java)
                        if (currentApp?.providerEmail == application.providerEmail) {
                            applicationSnapshot.ref.child("status").setValue("Rejected")

                            // Send notification to service provider
                            sendRejectionNotification(application.providerEmail)

                            Toast.makeText(context, "Application rejected", Toast.LENGTH_SHORT).show()
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to reject application", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun sendAcceptanceNotification(providerEmail: String) {
        // Get client's name
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .child("name")
                .get()
                .addOnSuccessListener { nameSnapshot ->
                    val clientName = nameSnapshot.getValue(String::class.java) ?: "Client"

                    // Get provider's user ID
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .orderByChild("email")
                        .equalTo(providerEmail)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val providerId = snapshot.children.firstOrNull()?.key ?: return
                                // Create notification
                                val notification = NotificationModel(
                                    id = FirebaseDatabase.getInstance().reference.push().key
                                        ?: return,
                                    title = "Application Accepted",
                                    description = "$clientName has accepted your application. Please wait for them to book your service.",
                                    type = "application_status",
                                    senderId = currentUser.uid,
                                    senderName = clientName,
                                    timestamp = System.currentTimeMillis(),
                                    postId = postId,
                                    isRead = false
                                )

                                // Save notification
                                FirebaseDatabase.getInstance().reference
                                    .child("notifications")
                                    .child(providerId)
                                    .child(notification.id)
                                    .setValue(notification)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                    "ApplicantsList",
                                    "Error finding provider",
                                    error.toException()
                                )
                            }
                        })
                }
        }
    }

    private fun sendRejectionNotification(providerEmail: String) {
        // Get client's name
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .child("name")
                .get()
                .addOnSuccessListener { nameSnapshot ->
                    val clientName = nameSnapshot.getValue(String::class.java) ?: "Client"

                    // Get provider's user ID
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .orderByChild("email")
                        .equalTo(providerEmail)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val providerId = snapshot.children.firstOrNull()?.key ?: return
                                // Create notification
                                val notification = NotificationModel(
                                    id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                    title = "Application Rejected",
                                    description = "Sorry, $clientName has rejected your application. Perhaps they've already accepted someone else. Don't worry, there are more clients who might post services similar to what you offer.",
                                    type = "application_status",
                                    senderId = currentUser.uid,
                                    senderName = clientName,
                                    timestamp = System.currentTimeMillis(),
                                    postId = postId,
                                    isRead = false
                                )

                                // Save notification
                                FirebaseDatabase.getInstance().reference
                                    .child("notifications")
                                    .child(providerId)
                                    .child(notification.id)
                                    .setValue(notification)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ApplicantsList", "Error finding provider", error.toException())
                            }
                        })
                }
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(postId: String, serviceOffered: String) =
            ApplicantsListFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putString("SERVICE_OFFERED", serviceOffered)
                }
            }
    }
}