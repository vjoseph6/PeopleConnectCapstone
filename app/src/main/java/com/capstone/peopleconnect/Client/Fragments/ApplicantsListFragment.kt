package com.capstone.peopleconnect.Client.Fragments

import ApplicantsAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

class ApplicantsListFragment : Fragment() {
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
                database.child("users")
                    .orderByChild("email")
                    .equalTo(providerEmail)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (userSnapshot in snapshot.children) {
                                val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                                val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                                callback(name, profileImageUrl)
                                break
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, "Failed to load user details", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
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
                        if (application?.status == "Pending") {
                            applicants.add(application)
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
        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (applicationSnapshot in snapshot.children) {
                        val currentApp = applicationSnapshot.getValue(PostApplication::class.java)
                        if (currentApp?.providerEmail == application.providerEmail) {
                            applicationSnapshot.ref.child("status").setValue("Accepted")
                            Toast.makeText(context, "Application accepted", Toast.LENGTH_SHORT).show()
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to accept application", Toast.LENGTH_SHORT).show()
                }
            })
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