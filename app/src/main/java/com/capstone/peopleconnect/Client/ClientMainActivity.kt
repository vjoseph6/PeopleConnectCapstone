package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
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

    private lateinit var bottomNavigationView: BottomNavigationView

    private var backPressedOnce = false

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
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load the HomeFragmentClient as the default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragmentClient(),  "home", firstName, middleName, lastName, userName, address, email, profileImage)
        }

        // Set the item selected listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragmentClient(), "home", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.activities -> {
                    loadFragment(ActivityFragmentClient(), "activities", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.mic -> {
                    loadFragment(MicFragmentClient(), "mic", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.categories -> {
                    loadFragment(CategoryFragmentClient(), "categories", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragmentClient(), "profile", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                else -> false
            }
        }

    }

    override fun onBackPressed() {
        // Get the current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)

        // If the current fragment is CategoryFragmentClient, let the fragment handle the back press
        if (currentFragment is CategoryFragmentClient) {
            (currentFragment as CategoryFragmentClient).handleFragmentBackPress()
        } else {
            // Handle back press normally
            if (backPressedOnce) {
                finishAffinity()  // Exit the app
                super.onBackPressed()
            } else {
                Toast.makeText(this, "Press back again to exit the application", Toast.LENGTH_SHORT).show()
                backPressedOnce = true
                Handler().postDelayed({ backPressedOnce = false }, 2000)
            }
        }
    }



    private fun loadFragment(fragment: Fragment, tag: String, firstName: String?, middleName: String?, lastName: String?, userName: String?, userAddress: String?, email: String?, profileImageUrl: String?) {
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

        // Get the FragmentTransaction
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment, tag)

        // Add the transaction to the back stack
        transaction.addToBackStack(tag)

        // Commit the transaction
        transaction.commit()
    }
}