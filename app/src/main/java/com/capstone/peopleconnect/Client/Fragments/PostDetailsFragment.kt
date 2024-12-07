package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.PostImageAdapter
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.Classes.PostApplication
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.capstone.peopleconnect.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query


class PostDetailsFragment : Fragment() {

    private var categoryName: String? = null
    private var bookDay: String? = null
    private var startDate: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private var description: String? = null
    private var images: ArrayList<String>? = null
    private var isFromProvider: Boolean = false
    private var providerEmail: String? = null
    private var postId: String? = null
    private lateinit var imageViewPager: ViewPager2
    private lateinit var imageAdapter: PostImageAdapter
    private lateinit var database: DatabaseReference
    private var clientEmail: String? = null
    private var postStatusListener: ValueEventListener? = null
    private var postStatusQuery: Query? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        arguments?.let {
            categoryName = it.getString("CATEGORY")
            bookDay = it.getString("BOOK_DAY")
            startDate = it.getString("START_DATE")
            startTime = it.getString("START_TIME")
            endTime = it.getString("END_TIME")
            description = it.getString("DESCRIPTION")
            images = it.getStringArrayList("IMAGES")
            isFromProvider = it.getBoolean("IS_FROM_PROVIDER", false)
            providerEmail = it.getString("PROVIDER_EMAIL")
            postId = it.getString("POST_ID")
            clientEmail = it.getString("CLIENT_EMAIL")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_post_details, container, false)

        // Initialize views
        val statusView: TextView = view.findViewById(R.id.statusView)
        imageViewPager = view.findViewById(R.id.imageViewPager)
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        val descriptionLabel: TextView = view.findViewById(R.id.descriptionLabel)
        val topBar: View = view.findViewById(R.id.topBar)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)

        // Common functionality for updating statusView
        fun updateStatusView(postStatus: String) {
            statusView.text = "Status: ${postStatus.capitalize()}"

            // Change text color to red if status is Closed
            if (postStatus.equals("Closed", ignoreCase = true)) {
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            } else {
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            }
        }

        // Store the query and listener for potential removal
        postStatusQuery = database.child("posts").child(postId!!)
        postStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Explicit null and attachment checks
                if (!isAdded || context == null) return

                val postStatus = snapshot.child("status").getValue(String::class.java) ?: "Open"

                // Ensure UI updates happen on the main thread
                activity?.runOnUiThread {
                    updateStatusView(postStatus)

                    // Show "application rejected" toast only for provider view
                    if (isFromProvider && postStatus.equals("Closed", ignoreCase = true)) {
                        showSafeToast(
                            "Your application has been rejected",
                            Toast.LENGTH_SHORT
                        )
                    }

                    if (isFromProvider) {
                        val applyBtn = view.findViewById<Button>(R.id.btnApply)

                        if (postStatus.equals("Closed", ignoreCase = true)) {
                            applyBtn.apply {
                                text = "Post Closed"
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                                isEnabled = false
                                setOnClickListener {
                                    showSafeToast("Post is closed")
                                }
                            }
                        } else {
                            applyBtn.apply {
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
                                visibility = View.VISIBLE
                                isEnabled = true
                                text = "Apply"
                                setOnClickListener {
                                    applyForPost()
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showSafeToast("Failed to load post status")
            }
        }

        // Add the listener
        postStatusQuery?.addValueEventListener(postStatusListener!!)

        if (isFromProvider) {
            // Change colors to blue for provider view
            topBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
            descriptionLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))

            // Update calendar and clock icons to blue versions
            view.findViewById<ImageView>(R.id.calendarIcon)?.setImageResource(R.drawable.ic_calendar_provider)
            view.findViewById<ImageView>(R.id.clockIcon)?.setImageResource(R.drawable.ic_clock_provider)
            view.findViewById<ImageButton>(R.id.btnBack)?.setImageResource(R.drawable.backbtn2_sprovider_)
            view.findViewById<TextView>(R.id.categoryText)?.setBackgroundResource(R.drawable.category_badge_background_provider)
        } else {
            val closePostTV: Button = view.findViewById(R.id.closePost)
            closePostTV.visibility = View.VISIBLE
            closePostTV.setOnClickListener {
                handleCloseApplication { updatedStatus ->
                    // Update the button text based on the post status
                    closePostTV.text = if (updatedStatus == "Closed") "Reopen Post" else "Close Post"
                }
            }

            val applyBtn = view.findViewById<Button>(R.id.btnApply)
            applyBtn.visibility = View.GONE

            // Add View Applicants button for client view
            val btnViewApplicants = view.findViewById<TextView>(R.id.btnViewApplicants)
            btnViewApplicants.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    val applicantsFragment = ApplicantsListFragment.newInstance(
                        postId = postId!!,
                        serviceOffered = categoryName!!
                    )
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, applicantsFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        if (!isFromProvider) {
            val closePostTV: Button = view.findViewById(R.id.closePost)
            closePostTV.visibility = View.VISIBLE

            // Track the current post status by querying the database
            var currentPostStatus = "Open" // Default initial state

            // Function to update button based on current status
            fun updateClosePostButton() {
                database.child("posts").child(postId!!).child("status")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            currentPostStatus = snapshot.getValue(String::class.java) ?: "Open"
                            closePostTV.text = if (currentPostStatus == "Closed") "Reopen Post" else "Close Post"
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                            closePostTV.text = "Close Post"
                        }
                    })
            }

            // Initial button state setup
            updateClosePostButton()

            closePostTV.setOnClickListener {
                if (currentPostStatus == "Open") {
                    // Close Post Logic
                    handleCloseApplication { updatedStatus ->
                        currentPostStatus = updatedStatus
                        updateClosePostButton()
                    }
                } else {
                    // Reopen Post Logic
                    reopenPost { reopenStatus ->
                        currentPostStatus = reopenStatus
                        updateClosePostButton()
                    }
                }
            }
        }


        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Initialize ViewPager with click handler
        imageAdapter = PostImageAdapter(images ?: emptyList()) { imageUrl ->
            showFullscreenImage(imageUrl)
        }
        imageViewPager.adapter = imageAdapter

        // Populate data
        categoryText.text = categoryName
        dateText.text = startDate
        timeText.text = "$startTime - $endTime"
        descriptionText.text = description



        // Set up TabLayout
        val tabLayout: TabLayout = view.findViewById(R.id.imageIndicator)
        TabLayoutMediator(tabLayout, imageViewPager) { _, _ -> }.attach()

        return view
    }

    // Remove listener to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()

        // Remove the listener if it exists
        postStatusQuery?.removeEventListener(postStatusListener!!)
        postStatusListener = null
        postStatusQuery = null
    }

    // Safe toast method with additional checks
    private fun showSafeToast(message: String, length: Int = Toast.LENGTH_SHORT) {
        // Check if the fragment is attached and the context is available
        if (isAdded && context != null) {
            activity?.runOnUiThread {
                try {
                    Toast.makeText(requireContext(), message, length).show()
                } catch (e: IllegalStateException) {
                    // Log the error or handle it silently
                    Log.e("PostDetailsFragment", "Failed to show toast", e)
                }
            }
        }
    }

    private fun handleCloseApplication(onStatusUpdated: ((String) -> Unit)? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.client_dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvLogoutTitle)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnLogout)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)

        tvTitle.text = "Do you want to close this post?"
        btnConfirm.text = "Close"

        btnConfirm.setOnClickListener {
            if (postId != null) {
                database.child("posts").child(postId!!).child("status").setValue("Closed")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Post closed successfully", Toast.LENGTH_SHORT).show()

                        // Update pending applications to rejected
                        database.child("post_applicants")
                            .orderByChild("postId")
                            .equalTo(postId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (applicationSnapshot in snapshot.children) {
                                        val currentApplication = applicationSnapshot.getValue(PostApplication::class.java)

                                        // Only reject pending applications, keep accepted and rejected as is
                                        if (currentApplication?.status == "Pending") {
                                            applicationSnapshot.ref.child("status").setValue("Rejected")
                                        }
                                    }
                                    onStatusUpdated?.invoke("Closed")
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "Failed to update applications", Toast.LENGTH_SHORT).show()
                                    onStatusUpdated?.invoke("Error")
                                }
                            })

                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to close post", Toast.LENGTH_SHORT).show()
                        onStatusUpdated?.invoke("Error")
                    }
            }
            dialog.dismiss()
        }

        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun reopenPost(onStatusUpdated: ((String) -> Unit)? = null) {
        if (postId != null) {
            database.child("posts").child(postId!!).child("status").setValue("Open")
                .addOnSuccessListener {
                    Toast.makeText(context, "Post reopened successfully", Toast.LENGTH_SHORT).show()

                    // Optionally, you can add logic to update rejected applications
                    database.child("post_applicants")
                        .orderByChild("postId")
                        .equalTo(postId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (applicationSnapshot in snapshot.children) {
                                    val currentApplication = applicationSnapshot.getValue(PostApplication::class.java)

                                    // Optional: You can decide whether to reset rejected applications
                                    // This is commented out as per your requirements, but can be adjusted
                                    // if (currentApplication?.status == "Rejected") {
                                    //     applicationSnapshot.ref.child("status").setValue("Pending")
                                    // }
                                }
                                onStatusUpdated?.invoke("Open")
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "Failed to update applications", Toast.LENGTH_SHORT).show()
                                onStatusUpdated?.invoke("Error")
                            }
                        })

                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to reopen post", Toast.LENGTH_SHORT).show()
                    onStatusUpdated?.invoke("Error")
                }
        }
    }


    private fun showFullscreenImage(imageUrl: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val photoView = dialog.findViewById<PhotoView>(R.id.fullscreenImageView)
        val closeButton = dialog.findViewById<ImageButton>(R.id.btnClose)

        Glide.with(requireContext())
            .load(imageUrl)
            .into(photoView)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyForPost() {
        if (postId == null || providerEmail == null || clientEmail == null) {
            return
        }

        val applyButton = view?.findViewById<Button>(R.id.btnApply)
        val statusView: TextView? = view?.findViewById(R.id.statusView)

        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var existingApplication: PostApplication? = null
                    var applicationKey: String? = null
                    var postHasAcceptedApplication = false
                    var userAcceptedApplication: PostApplication? = null
                    var postStatus = "Open" // Default to Open

                    // First, check the post status
                    database.child("posts").child(postId!!).child("status")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(postStatusSnapshot: DataSnapshot) {
                                postStatus = postStatusSnapshot.getValue(String::class.java) ?: "Open"

                                // Now process applications
                                for (applicationSnapshot in snapshot.children) {
                                    val application = applicationSnapshot.getValue(PostApplication::class.java)

                                    // Check if there's an accepted application for this post
                                    if (application?.status == "Accepted") {
                                        postHasAcceptedApplication = true

                                        // Check if current provider's application is accepted
                                        if (application.providerEmail == providerEmail) {
                                            userAcceptedApplication = application
                                        }
                                        break
                                    }

                                    // Check if provider has already applied
                                    if (application?.providerEmail == providerEmail) {
                                        existingApplication = application
                                        applicationKey = applicationSnapshot.key
                                    }
                                }

                                // Update status view and apply button
                                updateStatusAndApplyButton(
                                    statusView,
                                    applyButton,
                                    existingApplication,
                                    applicationKey,
                                    postHasAcceptedApplication,
                                    userAcceptedApplication,
                                    postStatus
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    context,
                                    "Failed to check post status",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        context,
                        "Failed to check application status",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateStatusAndApplyButton(
        statusView: TextView?,
        applyButton: Button?,
        existingApplication: PostApplication?,
        applicationKey: String?,
        postHasAcceptedApplication: Boolean,
        userAcceptedApplication: PostApplication?,
        postStatus: String
    ) {
        // Update status view text and color
        statusView?.apply {
            text = "Status: ${postStatus.capitalize()}"
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (postStatus == "Closed") R.color.red else R.color.green
                )
            )
        }

        applyButton?.apply {
            when {
                // Current provider's application is accepted (regardless of post status)
                userAcceptedApplication != null -> {
                    text = "Accepted"
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                    isEnabled = false
                    visibility = View.VISIBLE
                }

                // Post has an accepted application (not by current provider)
                postHasAcceptedApplication -> {
                    text = "Assigned"
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
                    isEnabled = false
                    visibility = View.VISIBLE
                }

                // Provider has a pending application
                existingApplication?.status == "Pending" -> {
                    text = "Cancel Application"
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                    isEnabled = true
                    visibility = View.VISIBLE
                    setOnClickListener {
                        showCancelDialog(applicationKey)
                    }
                }

                // Provider's application was rejected
                existingApplication?.status == "Rejected" -> {
                    when {
                        // If post is closed, show Rejected with disabled button
                        postStatus == "Closed" -> {
                            text = "Rejected"
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                            isEnabled = false
                        }

                        // If post is open, allow reapplication
                        postStatus == "Open" -> {
                            text = "Reapply"
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
                            isEnabled = true
                            setOnClickListener {
                                reapplyForPost(applicationKey)
                            }
                        }

                        // Fallback
                        else -> {
                            text = "Rejected"
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                            isEnabled = false
                        }
                    }
                    visibility = View.VISIBLE
                }

                // No existing application or previous applications
                else -> {
                    text = "Apply"
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
                    isEnabled = true
                    visibility = View.VISIBLE
                    setOnClickListener {
                        createNewApplication()
                    }
                }
            }
        }

        // Modify the toast logic in the status change listener
        if (isFromProvider && postStatus == "Closed" && userAcceptedApplication == null) {
            showSafeToast("Your application has been rejected", Toast.LENGTH_SHORT)
        }
    }

    private fun reapplyForPost(applicationKey: String?) {
        applicationKey?.let { key ->
            database.child("post_applicants").child(key).child("status").setValue("Pending")
                .addOnSuccessListener {
                    Toast.makeText(context, "Reapplied successfully", Toast.LENGTH_SHORT).show()
                    applyForPost() // Refresh the button state
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to reapply", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showCancelDialog(applicationKey: String?) {
        if (applicationKey == null) return

        val dialogView = layoutInflater.inflate(R.layout.sprovider_dialog_logout, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)

        val tvLogoutTitle = dialogView.findViewById<TextView>(R.id.tvLogoutTitle)
        tvLogoutTitle.text = "Are you sure you want to cancel this application?"

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        dialog.show()

        // Ensure this is a Button, not a TextView
        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)
        btnLogout.text = "Yes, cancel application"
        btnLogout.setTextAppearance(requireContext(), R.style.BlueButtonStyle)  // Apply style correctly
        tvLogoutTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))

        val btnCancel = dialogView.findViewById<TextView>(R.id.tvCancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnLogout.setOnClickListener {
            cancelApplication(applicationKey)
            dialog.dismiss()
        }

        dialog.show()
    }



    private fun cancelApplication(applicationKey: String) {
        database.child("post_applicants").child(applicationKey).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Application canceled", Toast.LENGTH_SHORT).show()
                applyForPost() // Refresh the button state
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to cancel application", Toast.LENGTH_SHORT).show()
            }
    }


    private fun createNewApplication() {
        val application = PostApplication(
            postId = postId!!,
            providerEmail = providerEmail!!,
            clientEmail = clientEmail!!,
            status = "Pending"
        )

        database.child("post_applicants").push().setValue(application)
            .addOnSuccessListener {
                // Get provider's name for the notification
                FirebaseDatabase.getInstance().reference
                    .child("users")
                    .orderByChild("email")
                    .equalTo(providerEmail)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val providerName = snapshot.children.firstOrNull()
                                ?.child("name")?.getValue(String::class.java) ?: "Service Provider"

                            // Get client's user ID
                            FirebaseDatabase.getInstance().reference
                                .child("users")
                                .orderByChild("email")
                                .equalTo(clientEmail)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(clientSnapshot: DataSnapshot) {
                                        val clientId = clientSnapshot.children.firstOrNull()?.key ?: return

                                        // Create notification
                                        val notification = NotificationModel(
                                            id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                            title = "New Application",
                                            description = "$providerName has applied to your post. You might be interested in them.",
                                            type = "post_application",
                                            senderId = providerEmail!!,
                                            senderName = providerName,
                                            timestamp = System.currentTimeMillis(),
                                            postId = postId,
                                            isRead = false
                                        )

                                        // Save notification for client
                                        FirebaseDatabase.getInstance().reference
                                            .child("notifications")
                                            .child(clientId)
                                            .child(notification.id)
                                            .setValue(notification)
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("PostDetails", "Error finding client", error.toException())
                                    }
                                })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("PostDetails", "Error getting provider name", error.toException())
                        }
                    })

                Toast.makeText(context, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                applyForPost() // Refresh the button state
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to submit application", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        applyForPost() // Update the button state when the fragment becomes active
    }




    companion object {
        @JvmStatic
        fun newInstance(post: Post, isFromProvider: Boolean = false, providerEmail: String? = null) =
            PostDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("CATEGORY", post.categoryName)
                    putString("BOOK_DAY", post.bookDay)
                    putString("START_DATE", post.startDate)
                    putString("START_TIME", post.startTime)
                    putString("END_TIME", post.endTime)
                    putString("DESCRIPTION", post.postDescription)
                    putStringArrayList("IMAGES", ArrayList(post.postImages))
                    putBoolean("IS_FROM_PROVIDER", isFromProvider)
                    putString("PROVIDER_EMAIL", providerEmail)
                    putString("POST_ID", post.postId)
                    putString("CLIENT_EMAIL", post.email)
                }
            }
    }
}