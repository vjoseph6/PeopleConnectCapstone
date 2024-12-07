package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.ClientPostAdapter
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.Client.Fragments.PostDetailsFragment
import com.capstone.peopleconnect.Helper.NotificationHelper
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.capstone.peopleconnect.R
import com.google.android.material.bottomsheet.BottomSheetDialog
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


class HomeFragmentSProvider : Fragment() {

    private val TAG = "HomeFragmentSProvider"
    private var email: String? = null
    private var nameTextView: TextView? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ClientPostAdapter
    private lateinit var notificationBadgeSProvider: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
        }
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_s_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add after existing view initialization
        notificationBadgeSProvider = view.findViewById(R.id.notificationBadge_sprovider)
        setupNotificationBadge()

        updateDateText(view)

        nameTextView = view.findViewById(R.id.tvName)


        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rvInterests)
        emptyView = view.findViewById(R.id.emptyView)

        setupRecyclerView()

        val currentEmail = email ?: return
        setupIconClickListeners(view)
        fetchUserData(currentEmail)
        retrieveUserSkills(currentEmail)
    }

    private fun setupRecyclerView() {
        adapter = ClientPostAdapter(
            onPostClick = { post ->
                val postDetailsFragment = PostDetailsFragment.newInstance(
                    post = post,
                    isFromProvider = true,
                    providerEmail = email
                )
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, postDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            },
            isFromProvider = true
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragmentSProvider.adapter
        }
    }

    private fun setupNotificationBadge() {
        NotificationHelper.setupNotificationBadge(
            fragment = this,
            notificationBadge = notificationBadgeSProvider,
            tag = TAG
        )
    }

    private fun fetchUserData(providerEmail: String) {
        val userReference = FirebaseDatabase.getInstance().getReference("users")
        userReference.orderByChild("email").equalTo(providerEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        nameTextView?.text = userSnapshot.child("firstName").value as? String
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }



    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())
        view.findViewById<TextView>(R.id.tvDate_SPROVIDER).text = currentDate
    }

    private fun setupIconClickListeners(view: View) {
        val ivFilter: ImageView = view.findViewById(R.id.ivFilter)
        ivFilter.setOnClickListener { showFilterDialog() }

        view.findViewById<LinearLayout>(R.id.notificationLayout_sprovider).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, NotificationFragmentSProvider())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<LinearLayout>(R.id.messageLayout_sprovider).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, MessageFragmentSProvider())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.sprovider_dialog_filter_options, null)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(dialogView)

        // Handle button clicks with a delay
        listOf(R.id.btnToday, R.id.btnTomorrow, R.id.btnUpcoming).forEach { buttonId ->
            dialogView.findViewById<Button>(buttonId).setOnClickListener {
                it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
                it.postDelayed({ bottomSheetDialog.dismiss() }, 200)
            }
        }

        bottomSheetDialog.show()
    }

    private fun retrieveUserSkills(email: String) {
        val skillsReference = database.child("skills").orderByChild("user").equalTo(email)

        skillsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userSkills = mutableSetOf<String>()

                for (skillSnapshot in snapshot.children) {
                    val skillItemsSnapshot = skillSnapshot.child("skillItems")
                    for (skillItem in skillItemsSnapshot.children) {
                        val skillName = skillItem.child("name").getValue(String::class.java)
                        skillName?.let { userSkills.add(it) }
                    }
                }

                if (userSkills.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    retrieveMatchingPosts(userSkills)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load skills", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveMatchingPosts(userSkills: Set<String>) {
        val postsReference = database.child("posts")
        val currentEmail = email // Get the current user's email

        postsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchingPosts = mutableListOf<Post>()

                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)

                    if (post != null &&
                        post.client &&
                        userSkills.contains(post.categoryName) &&
                        post.email != currentEmail
                    ) {
                        Log.d(TAG, "Valid post found: ${post.postId}, category: ${post.categoryName}")
                        if (post.postStatus == "Approved") {
                            sendNotificationToProvider(post)
                            matchingPosts.add(post)
                        }
                    } else {
                        Log.d(TAG, "Post skipped: ${post?.postId}, reason: does not meet conditions")
                    }
                }

            if (matchingPosts.isEmpty()) {
                    showEmptyView()
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    adapter.updatePosts(matchingPosts)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendNotificationToProvider(post: Post) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Reference to notifications for the current user
        val notificationsRef = FirebaseDatabase.getInstance().reference
            .child("notifications")
            .child(currentUser.uid)

        // Query to check if a notification for this post already exists
        notificationsRef.orderByChild("postId")
            .equalTo(post.postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Count how many matching notifications exist
                    val matchingNotifications = snapshot.children.count {
                        it.child("postId").getValue(String::class.java) == post.postId
                    }

                    if (matchingNotifications == 0) {
                        Log.d(TAG, "No existing notification found for postId: ${post.postId}. Creating one.")

                        FirebaseDatabase.getInstance().reference
                            .child("users")
                            .orderByChild("email")
                            .equalTo(post.email)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val clientName = userSnapshot.children.firstOrNull()
                                        ?.child("name")?.getValue(String::class.java) ?: "Client"

                                    val notification = NotificationModel(
                                        id = notificationsRef.push().key ?: return,
                                        title = "New Service Request",
                                        description = "$clientName is looking for your service in ${post.categoryName}",
                                        type = "post_request",
                                        senderId = post.email ?: "",
                                        senderName = clientName,
                                        timestamp = System.currentTimeMillis(),
                                        postId = post.postId,
                                        isRead = false
                                    )

                                    notificationsRef.child(notification.id).setValue(notification)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Notification created successfully: ${notification.id}")
                                        }
                                        .addOnFailureListener {
                                            Log.e(TAG, "Failed to create notification", it)
                                        }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "Error fetching client details", error.toException())
                                }
                            })
                    } else {
                        Log.d(TAG, "Notification for postId: ${post.postId} already exists. Skipping creation.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error checking existing notifications", error.toException())
                }
            })
    }


    private fun showEmptyView() {
        if (!isAdded) {
            Log.w(TAG, "Fragment not attached to context, skipping showEmptyView")
            return
        }

        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE

        // Add animation to the empty view image
        val emptyImage = emptyView.findViewById<ImageView>(R.id.image)
        Glide.with(requireContext())
            .load(R.drawable.nothing) // Your empty state image
            .into(emptyImage)

        // Optional: Add some subtle animation
        emptyImage.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    companion object {

        @JvmStatic
        fun newInstance(email: String) =
            HomeFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}