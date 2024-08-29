package com.capstone.peopleconnect.SPrvoider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprovider_main)

        // Retrieve data from Intent
        val userName = intent.getStringExtra("USER_NAME")
        val fName = intent.getStringExtra("FIRST_NAME")
        val mName = intent.getStringExtra("MIDDLE_NAME")
        val lName = intent.getStringExtra("LAST_NAME")
        val userAddress = intent.getStringExtra("USER_ADDRESS")
        val email = intent.getStringExtra("EMAIL")
        val profileImageUrl = intent.getStringExtra("PROFILE_IMAGE_URL")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomeFragmentSProvider(),  fName, mName, lName, userName, userAddress, email, profileImageUrl)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragmentSProvider(), fName, mName, lName, userName, userAddress, email, profileImageUrl)
                    true
                }
                R.id.activities -> {
                    loadFragment(ActivityFragmentSProvider(), fName, mName, lName, userName, userAddress, email, profileImageUrl)
                    true
                }
                R.id.mic -> {
                    loadFragment(MicFragmentSProvider(), fName, mName, lName, userName, userAddress, email, profileImageUrl)
                    true
                }
                R.id.skills -> {
                    loadFragment(SkillsFragmentSProvider(), fName, mName, lName, userName, userAddress, email, profileImageUrl)
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragmentSProvider(), fName, mName, lName, userName, userAddress, email, profileImageUrl)
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
