package com.capstone.peopleconnect.SPrvoider.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.SkillsAdapter
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.Helper.NotificationHelper
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.AddSkillsProviderRate
import com.capstone.peopleconnect.SPrvoider.AddSkillsSProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SkillsFragmentSProvider : Fragment() {
    private lateinit var skillsAdapter: SkillsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private var email: String? = null
    private var profileImage: String? = null
    private lateinit var emptyView: RelativeLayout
    private val TAG = "SkillsFragmentSProvider"
    private lateinit var notificationBadgeSProvider: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
            profileImage = it.getString("PROFILE_IMAGE_URL") ?: ""
            Log.d("URL PASSED SKILLS", "$profileImage")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_skills_s_provider, container, false)


        // Add after existing view initialization
        notificationBadgeSProvider = view.findViewById(R.id.notificationBadge_sprovider)
        setupNotificationBadge()

        updateDateText(view)

        // Notification icons
        val notificationIcons: ImageView = view.findViewById(R.id.notificationIcons)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()

        }

        // Message icons
        val messageIcons: ImageView = view.findViewById(R.id.messageIcon)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()

        }

        // Set up RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.recyclerViewSkills)
        recyclerView.layoutManager = LinearLayoutManager(context)
        emptyView = view.findViewById(R.id.emptyView)

        // Load image into ImageView using Glide
        val emptyImage: ImageView = view.findViewById(R.id.image)
        Glide.with(this)
            .load(R.drawable.nothing)  // Replace with your drawable resource or image URL
            .into(emptyImage)

        // Initialize both views as GONE until data is loaded
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        skillsAdapter = SkillsAdapter(
            emptyList(),
            { updatedSkill -> updateSkillVisibilityInDatabase(updatedSkill) },
            { selectedSkill -> // Handle item click here
                val intent = Intent(requireContext(), AddSkillsProviderRate::class.java)
                intent.putExtra("PROFILE_IMAGE_URL", profileImage)
                intent.putExtra("SUBCATEGORY_NAME", selectedSkill.name) // Pass the skill name
                email?.let { intent.putExtra("EMAIL", it) } // Pass the email
                startActivity(intent)
            }
        )
        recyclerView.adapter = skillsAdapter

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Retrieve skills data
        email?.let { retrieveUserSkills(it) }

        //add skill button
        val addBtn = view.findViewById<ImageButton>(R.id.addBtn)
        addBtn.setOnClickListener {
            // Create the intent to start AddSkillsSProvider activity
            val intent = Intent(requireContext(), AddSkillsSProvider::class.java)

            // Check if email is not null before adding it to the intent
            email?.let { emailAddress ->
                intent.putExtra("EMAIL", emailAddress)
            }

            // Start the activity
            startActivity(intent)
        }

        return view
    }

    private fun setupNotificationBadge() {
        NotificationHelper.setupNotificationBadge(
            fragment = this,
            notificationBadge = notificationBadgeSProvider,
            tag = TAG
        )
    }

    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())

        // Find the TextView and set the formatted date
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        tvDate.text = currentDate // Set the formatted date to the TextView
    }

    private fun retrieveUserSkills(email: String) {
        val skillsReference = database.child("skills").orderByChild("user").equalTo(email)

        skillsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val skillsList = mutableListOf<SkillItem>()
                val skillItemsToFetch = snapshot.children.sumOf { it.child("skillItems").childrenCount.toInt() }
                var fetchedSkillCount = 0

                if (skillItemsToFetch.toLong() == 0L) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    return
                }

                for (skillSnapshot in snapshot.children) {
                    val skillItemsSnapshot = skillSnapshot.child("skillItems")

                    for (skillItem in skillItemsSnapshot.children) {
                        val skillName = skillItem.child("name").getValue(String::class.java) ?: ""
                        val isVisible = skillItem.child("visible").getValue(Boolean::class.java) ?: true

                        findImageForSkill(skillName) { imageUrl ->
                            val skillItemObj = SkillItem(name = skillName, visible = isVisible, image = imageUrl, rating = 0.0f)
                            skillsList.add(skillItemObj)

                            fetchedSkillCount++

                            if (fetchedSkillCount == skillItemsToFetch) {
                                skillsAdapter.updateSkillsList(skillsList)

                                if (skillsList.isNotEmpty()) {
                                    recyclerView.visibility = View.VISIBLE
                                    emptyView.visibility = View.GONE
                                } else {
                                    recyclerView.visibility = View.GONE
                                    emptyView.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error retrieving skills: ${error.message}")
            }
        })
    }




    private fun findImageForSkill(skillName: String, callback: (String) -> Unit) {
        database.child("category").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (category in snapshot.children) {
                    val subCategoriesSnapshot = category.child("Sub Categories")

                    // Search in each subcategory for the matching skill name
                    for (subCategory in subCategoriesSnapshot.children) {
                        val subName = subCategory.child("name").getValue(String::class.java)
                        val image = subCategory.child("image").getValue(String::class.java)

                        if (subName == skillName) {
                            // Found the image for the skill, return it via the callback
                            Log.d(TAG, "Match Found: $subName, Image: $image")
                            callback(image ?: "")
                            return // Exit loop once a match is found
                        }
                    }
                }
                // If no match found, return an empty string
                callback("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error retrieving categories: ${error.message}")
                callback("") // Return empty string on error
            }
        })
    }

    private fun updateSkillVisibilityInDatabase(skill: SkillItem) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("skills")

        // Find the skill item in the database
        databaseReference.orderByChild("user").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (skillSnapshot in snapshot.children) {
                    val skillItemsSnapshot = skillSnapshot.child("skillItems")

                    for (skillItemSnapshot in skillItemsSnapshot.children) {
                        val skillName = skillItemSnapshot.child("name").getValue(String::class.java)

                        if (skillName == skill.name) {
                            skillItemSnapshot.ref.child("visible").setValue(skill.visible)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Skill visibility updated successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to update skill visibility", e)
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
            }
        })
    }
}
