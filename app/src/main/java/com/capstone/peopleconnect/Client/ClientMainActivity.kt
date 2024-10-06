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
import com.capstone.peopleconnect.Helper.SpeechRecognitionHelper
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.HomeFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.SkillsFragmentSProvider
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ClientMainActivity : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var userName: String
    private lateinit var middleName: String
    private lateinit var lastName: String
    private lateinit var address: String
    private lateinit var profileImage: String
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var speechRecognitionHelper: SpeechRecognitionHelper
    private var isListening = false
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_main)

        val btnToggleListening: FloatingActionButton = findViewById(R.id.btnToggleListening)

        // Initialize the speech recognition helper
        speechRecognitionHelper = SpeechRecognitionHelper(this)

        // Request microphone permission if not already granted
        speechRecognitionHelper.requestMicrophonePermission(this)

        // Set the button click listener for starting/stopping speech recognition
        btnToggleListening.setOnClickListener {
            if (isListening) {
                speechRecognitionHelper.stopSpeechToText()
                isListening = false
            } else {
                // Check if the recognizer is initialized
                if (::speechRecognitionHelper.isInitialized) {
                    speechRecognitionHelper.startSpeechToText()
                    isListening = true
                } else {
                    Log.d("ClientMainActivity", "SpeechRecognizer not initialized yet.")
                }
            }
        }

        email = intent.getStringExtra("EMAIL") ?: ""
        firstName = intent.getStringExtra("FIRST_NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        middleName = intent.getStringExtra("MIDDLE_NAME") ?: ""
        lastName = intent.getStringExtra("LAST_NAME") ?: ""
        address = intent.getStringExtra("USER_ADDRESS") ?: ""
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL") ?: ""

        // Reference to BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Check if an intent extra is provided to load a specific fragment
        val fragmentToLoad = intent.getStringExtra("FRAGMENT_TO_LOAD")
        if (fragmentToLoad != null) {
            when (fragmentToLoad) {
                "CategoryFragmentClient" -> {
                    val email = intent.getStringExtra("EMAIL")
                    loadFragment(CategoryFragmentClient(), "categories", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.categories
                }
                // Handle other fragments if needed
            }
        } else if (savedInstanceState == null) {
            // Default fragment
            loadFragment(HomeFragmentClient(), "home", firstName, middleName, lastName, userName, address, email, profileImage)
            bottomNavigationView.selectedItemId = R.id.home
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Pass the permission result to the helper
        speechRecognitionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)

        if (currentFragment is CategoryFragmentClient) {
            (currentFragment as CategoryFragmentClient).handleFragmentBackPress()
        } else {
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
        val bundle = Bundle().apply {
            putString("USER_NAME", userName)
            putString("FIRST_NAME", firstName)
            putString("MIDDLE_NAME", middleName)
            putString("LAST_NAME", lastName)
            putString("USER_ADDRESS", userAddress)
            putString("EMAIL", email)
            putString("PROFILE_IMAGE_URL", profileImageUrl)
        }

        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment, tag)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionHelper.destroy() // Clean up resources
    }
}

