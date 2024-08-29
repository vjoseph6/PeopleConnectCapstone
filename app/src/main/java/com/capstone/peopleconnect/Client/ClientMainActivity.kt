package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient
import com.capstone.peopleconnect.Client.Fragments.CategoryFragmentClient
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient
import com.capstone.peopleconnect.Client.Fragments.MicFragmentClient
import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.HomeFragmentSProvider
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView


class ClientMainActivity : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var userName: String
    private lateinit var middleName: String
    private lateinit var lastName: String
    private lateinit var address: String
    private lateinit var profileImage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_main)

        email = intent.getStringExtra("EMAIL") ?: ""
        firstName = intent.getStringExtra("FIRST_NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        middleName = intent.getStringExtra("MIDDLE_NAME") ?: ""
        lastName = intent.getStringExtra("LAST_NAME") ?: ""
        address = intent.getStringExtra("USER_ADDRESS") ?: ""
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL") ?: ""


        // Reference to BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load the HomeFragmentClient as the default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
        }

        // Set the item selected listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.activities -> {
                    loadFragment(ActivityFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.mic -> {
                    loadFragment(MicFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.categories -> {
                    loadFragment(CategoryFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragmentClient(),  firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment, firstName: String?, middleName: String?, lastName: String?, userName: String?, userAddress: String?, email: String?, profileImageUrl: String?) {
        // Create a new Bundle and add the data
        val bundle = Bundle().apply {
            putString("USER_NAME", userName)
            putString("FIRST_NAME", firstName)
            putString("MIDDLE_NAME", middleName)
            putString("LAST_NAME", lastName)
            putString("USER_ADDRESS", userAddress)
            putString("EMAIL", email)
            putString("PROFILE_IMAGE_URL", profileImageUrl)
        }

        // Set the arguments on the fragment
        fragment.arguments = bundle

        // Replace the current fragment with the new one
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}