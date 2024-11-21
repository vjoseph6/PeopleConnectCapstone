package com.capstone.peopleconnect.Client.Fragments

import android.app.Dialog
import android.os.Bundle
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


class PostDetailsFragment : Fragment() {

    private var categoryName: String? = null
    private var bookDay: String? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        arguments?.let {
            categoryName = it.getString("CATEGORY")
            bookDay = it.getString("BOOK_DAY")
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
        imageViewPager = view.findViewById(R.id.imageViewPager)
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        val descriptionLabel: TextView = view.findViewById(R.id.descriptionLabel)
        val topBar: View = view.findViewById(R.id.topBar)
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)

        if (isFromProvider) {
            // Change colors to blue for provider view
            topBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
            descriptionLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
            
            // Update calendar and clock icons to blue versions
            view.findViewById<ImageView>(R.id.calendarIcon)?.setImageResource(R.drawable.ic_calendar_provider)
            view.findViewById<ImageView>(R.id.clockIcon)?.setImageResource(R.drawable.ic_clock_provider)
            view.findViewById<ImageButton>(R.id.btnBack)?.setImageResource(R.drawable.backbtn2_sprovider_)
            view.findViewById<TextView>(R.id.categoryText)?.setBackgroundResource(R.drawable.category_badge_background_provider)

            val applyBtn = view.findViewById<Button>(R.id.btnApply)
            applyBtn.apply {
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.blue)
                visibility = View.VISIBLE
                setOnClickListener {
                    applyForPost()
                }
            }

            // Check if provider has already applied
            checkExistingApplication(view)
        } else {
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
        dateText.text = bookDay
        timeText.text = "$startTime - $endTime"
        descriptionText.text = description

        // Set up TabLayout
        val tabLayout: TabLayout = view.findViewById(R.id.imageIndicator)
        TabLayoutMediator(tabLayout, imageViewPager) { _, _ -> }.attach()

        return view
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
            Toast.makeText(context, "Unable to apply for post", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if already applied before creating new application
        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var alreadyApplied = false
                    for (applicationSnapshot in snapshot.children) {
                        val application = applicationSnapshot.getValue(PostApplication::class.java)
                        if (application?.providerEmail == providerEmail) {
                            alreadyApplied = true
                            break
                        }
                    }

                    if (!alreadyApplied) {
                        createNewApplication()
                    } else {
                        Toast.makeText(context, "You have already applied for this post", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to check application status", Toast.LENGTH_SHORT).show()
                }
            })
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
                Toast.makeText(context, "Application submitted successfully", Toast.LENGTH_SHORT).show()
                view?.findViewById<Button>(R.id.btnApply)?.apply {
                    isEnabled = false
                    text = "Applied"
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to submit application", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkExistingApplication(view: View) {
        if (postId == null || providerEmail == null) return

        database.child("post_applicants")
            .orderByChild("postId")
            .equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (applicationSnapshot in snapshot.children) {
                        val application = applicationSnapshot.getValue(PostApplication::class.java)
                        if (application?.providerEmail == providerEmail) {
                            // Provider has already applied
                            view.findViewById<Button>(R.id.btnApply)?.apply {
                                isEnabled = false
                                text = "Applied"
                            }
                            return
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to check application status", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        @JvmStatic
        fun newInstance(post: Post, isFromProvider: Boolean = false, providerEmail: String? = null) =
            PostDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("CATEGORY", post.categoryName)
                    putString("BOOK_DAY", post.bookDay)
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