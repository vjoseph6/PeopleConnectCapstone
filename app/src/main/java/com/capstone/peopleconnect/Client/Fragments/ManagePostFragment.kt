package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Adapters.ClientPostAdapter
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.Client.AddPostClientFragment
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.Glide

class ManagePostFragment : Fragment() {

    private var email: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: RelativeLayout
    private lateinit var adapter: ClientPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_post, container, false)

        recyclerView = view.findViewById(R.id.clientPosts)
        emptyView = view.findViewById(R.id.emptyView)
        
        setupRecyclerView()
        fetchPosts()

        // Handle back button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Handle add post button
        view.findViewById<ImageButton>(R.id.addPostBtn).setOnClickListener {
            val addPostFragment = AddPostClientFragment.newInstance(email.toString())
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, addPostFragment)
                .addToBackStack(null)
                .commit()
        }

        // Add this to load the GIF
        val emptyImage = view.findViewById<ImageView>(R.id.image)
        Glide.with(this)
            .asGif()
            .load(R.drawable.nothing) // Replace with your actual GIF resource
            .into(emptyImage)

        return view
    }

    private fun setupRecyclerView() {
         adapter = ClientPostAdapter(
            onPostClick = { post ->
                val postDetailsFragment = PostDetailsFragment.newInstance(post)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, postDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ManagePostFragment.adapter
        }
    }

    private fun fetchPosts() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("posts")
        databaseRef.orderByChild("email").equalTo(email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()
                    Log.d("FIREBASE", "Snapshot: ${snapshot.value}")

                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        Log.d("FIREBASE_POST", "Post: $post")
                        post?.let {
                            if (it.client) {
                                posts.add(it)
                            }
                        }
                    }

                    if (posts.isEmpty()) {
                        Log.d("FIREBASE", "No posts found for email: $email")
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        Log.d("FIREBASE", "Posts found: ${posts.size}")
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                        adapter.updatePosts(posts)
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {

        @JvmStatic
        fun newInstance(email: String?) =
            ManagePostFragment().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}