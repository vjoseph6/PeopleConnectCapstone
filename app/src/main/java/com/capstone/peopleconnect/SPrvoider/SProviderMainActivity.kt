package com.capstone.peopleconnect.SPrvoider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SProvider.Fragments.ProfileFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.ActivityFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.HomeFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.MicFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.SkillsFragmentSProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class SProviderMainActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_sprovider_main)

        email = intent.getStringExtra("EMAIL") ?: ""
        firstName = intent.getStringExtra("FIRST_NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        middleName = intent.getStringExtra("MIDDLE_NAME") ?: ""
        lastName = intent.getStringExtra("LAST_NAME") ?: ""
        address = intent.getStringExtra("USER_ADDRESS") ?: ""
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL") ?: ""

        // Initialize BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation)


        // Check if an intent extra is provided to load a specific fragment
        val fragmentToLoad = intent.getStringExtra("FRAGMENT_TO_LOAD")
        if (fragmentToLoad != null) {
            when (fragmentToLoad) {
                "SkillsFragmentSProvider" -> {
                    val email = intent.getStringExtra("EMAIL")
                    loadFragment(SkillsFragmentSProvider(), "skills", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.skills
                }
                "ActivityFragmentSProvider" -> {
                    val email = intent.getStringExtra("EMAIL")
                    loadFragment(ActivityFragmentSProvider(), "activities", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.activities
                }
                // Handle other fragments if needed
            }
        } else if (savedInstanceState == null) {
            // Default fragment
            loadFragment(HomeFragmentSProvider(), "home", firstName, middleName, lastName, userName, address, email, profileImage)
            bottomNavigationView.selectedItemId = R.id.home
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragmentSProvider(),  "home", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.activities -> {
                    loadFragment(ActivityFragmentSProvider(),"activities", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.skills -> {
                    loadFragment(SkillsFragmentSProvider(), "skills", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragmentSProvider(), "profile", firstName, middleName, lastName, userName, address, email, profileImage)
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)

        if (currentFragment is HomeFragmentSProvider
            || currentFragment is ActivityFragmentSProvider
            || currentFragment is SkillsFragmentSProvider
            || currentFragment is ProfileFragmentSProvider) {


            if (backPressedOnce) {
                finishAffinity()
                return
            } else {
                Toast.makeText(this, "Press back again to exit the application", Toast.LENGTH_SHORT).show()
                backPressedOnce = true
                Handler().postDelayed({ backPressedOnce = false }, 2000)
            }
        } else {
            super.onBackPressed() // Normal back press behavior for other fragments
        }
    }



    private fun loadFragment(fragment: Fragment, tag: String, firstName: String?, middleName: String?, lastName: String?, userName: String?, userAddress: String?, email: String?, profileImageUrl: String?) {
        val bundle = Bundle().apply {
            putString("USER_NAME", userName)
            putString("FIRST_NAME", firstName)
            putString("MIDDLE_NAME", middleName)
            putString("LAST_NAME", lastName)
            putString("USER_ADDRESS", userAddress)
            putString("EMAIL", email)
            putString("PROFILE_IMAGE_URL", profileImageUrl)  // Ensure profile image is passed here
        }
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment, tag)
            .commit()
    }



}
