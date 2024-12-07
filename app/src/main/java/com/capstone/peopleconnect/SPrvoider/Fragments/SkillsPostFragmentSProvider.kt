package com.capstone.peopleconnect.SPrvoider.Fragments

import SkillsPostsAdapter
import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Helper.ImagePreviewActivity
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SkillsPostFragmentSProvider : Fragment() {

    private lateinit var email: String
    private lateinit var categoryName: String
    private lateinit var recyclerView: RecyclerView
    private val postImages = mutableListOf<String>()
    private var tag: String? = null
    private var serviceType: String? = null
    private lateinit var adapter: SkillsPostsAdapter
    private lateinit var emptyView: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL").toString()
            categoryName = it.getString("CATEGORY_NAME").toString()
            tag = it.getString("isClient")
            serviceType = it.getString("serviceType")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_skills_post_s_provider, container, false)



        val textView: TextView = view.findViewById(R.id.tvSkills)
        textView.text = categoryName

        val addPost: ImageButton = view.findViewById(R.id.addPostBtn)
        val layout: ConstraintLayout = view.findViewById(R.id.pLayout)

        if (tag.equals("isClient", ignoreCase = true)) {  // Use case-insensitive comparison to avoid mismatches
            layout.setBackgroundResource(R.color.green)
            addPost.visibility = View.GONE

            val btnBack: ImageButton = view.findViewById(R.id.btnBack)
            btnBack.setImageResource(R.drawable.backbtn2_client_)  // Set the resource for the image
        }

        if (!serviceType.isNullOrBlank() && serviceType.equals(categoryName, ignoreCase = true)) {

            val newFragment = AddPostFragment.newInstance(email, categoryName)
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.frame_layout, newFragment) // Replace 'frame_layout' with the actual container ID
            transaction.addToBackStack(null) // This allows the user to navigate back to the current fragment
            transaction.commit()

        }



        addPost.setOnClickListener {
            val newFragment = AddPostFragment.newInstance(email, categoryName)
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.frame_layout, newFragment) // Replace 'frame_layout' with the actual container ID
            transaction.addToBackStack(null) // This allows the user to navigate back to the current fragment
            transaction.commit()
        }
        emptyView = view.findViewById(R.id.emptyView)

        // Load image into ImageView using Glide
        val emptyImage: ImageView = view.findViewById(R.id.image)
        Glide.with(this)
            .load(R.drawable.nothing)  // Replace with your drawable resource or image URL
            .into(emptyImage)

        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }


        recyclerView = view.findViewById(R.id.skillsPosts)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        // Set up the adapter with an image click listener to open full screen
        adapter = SkillsPostsAdapter(postImages) { imageUrl ->
            openFullScreenImage(imageUrl)
        }
        recyclerView.adapter = adapter

        loadPosts() // Load images from Firebase

        return view
    }

    private fun loadPosts() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("posts")
        databaseReference.orderByChild("email").equalTo(email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postImages.clear()

                    for (postSnapshot in snapshot.children) {
                        val postStatus = postSnapshot.child("postStatus").getValue(String::class.java)
                        val postCategory = postSnapshot.child("categoryName").getValue(String::class.java)

                        if (postStatus == "Approved" && postCategory == categoryName) {
                            val imagesSnapshot = postSnapshot.child("postImages")
                            for (imageSnapshot in imagesSnapshot.children) {
                                imageSnapshot.getValue(String::class.java)?.let { imageUrl ->
                                    postImages.add(imageUrl)
                                }
                            }
                        }
                    }

                    // Check if posts are available
                    if (postImages.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }

                    // Notify the adapter when data changes
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors if necessary
                }
            })
    }

    private fun openFullScreenImage(imageUrl: String) {
        // Start ImagePreviewActivity with the image URL and necessary data
        val intent = Intent(requireContext(), ImagePreviewActivity::class.java).apply {
            putExtra("IMAGE_URL", imageUrl)
            putExtra("EMAIL", email)
            putExtra("CATEGORY_NAME", categoryName)
            putExtra("ISCLIENT", tag)
        }
        startActivity(intent)
    }
    companion object {

        @JvmStatic
        fun newInstance(email: String?, categoryName: String?, isFromClient: String? = null, serviceType: String? = null) =
            SkillsPostFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("CATEGORY_NAME", categoryName)
                    isFromClient?.let { putString("isClient", it) }
                    serviceType?.let { putString("serviceType", it) }
                }
            }
    }
}