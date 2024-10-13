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
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.capstone.peopleconnect.Adapters.SpeechRecognitionCallback
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient
import com.capstone.peopleconnect.Client.Fragments.CategoryFragmentClient
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient
import com.capstone.peopleconnect.Client.Fragments.MicFragmentClient
import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.Helper.SpeechRecognitionHelper
import com.capstone.peopleconnect.Helper.WitAiHandler
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
    private lateinit var witAiHandler: WitAiHandler
    private var isListening = false
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_main)

        val btnToggleListening: FloatingActionButton = findViewById(R.id.btnToggleListening)
        witAiHandler = WitAiHandler(this@ClientMainActivity)

        // Initialize the speech recognition helper with a callback to handle speech results
        speechRecognitionHelper = SpeechRecognitionHelper(this, object : SpeechRecognitionCallback {
            override fun onSpeechResult(result: String) {
                // Send the recognized text to Wit.ai for further processing
                witAiHandler.sendMessageToWit(result, object : WitAiHandler.WitAiCallback {
                    // In your onResponse method
                    override fun onResponse(
                        bookDay: String,
                        startTime: String,
                        endTime: String,
                        rating: String,
                        serviceType: String,
                        intentName: String
                    ) {
                        Log.d("ClientMainActivity", "Wit.ai response: Day: $bookDay, Time: $startTime to $endTime, Rating: $rating, Service: $serviceType")

                        val name = "book_service"

                        // Create an additional bundle for other parameters
                        val additionalParams = Bundle().apply {
                            putString("bookDay", bookDay)
                            putString("startTime", startTime)
                            putString("endTime", endTime)
                            putString("rating", rating)
                            putString("serviceType", serviceType)
                        }

                        if (intentName == name) {
                            // Load CategoryFragmentClient and include email
                            loadFragment(CategoryFragmentClient(), "categories", firstName, middleName, lastName, userName, address, email, profileImage, additionalParams)
                            bottomNavigationView.selectedItemId = R.id.categories
                        }
                    }



                    override fun onError(errorMessage: String) {
                        Log.e("ClientMainActivity", "Wit.ai error: $errorMessage")
                    }
                })
            }

            override fun onError(error: String) {
                Toast.makeText(this@ClientMainActivity, "Error in speech recognition: $error", Toast.LENGTH_SHORT).show()
            }
        })

        // Request microphone permission if not already granted
        speechRecognitionHelper.requestMicrophonePermission(this)

        var isListening = false // Track listening state

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

        // Find the NavController associated with the FrameLayout
        var navHostFragment = supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment?
        if (navHostFragment == null) {
            navHostFragment = NavHostFragment.create(R.navigation.nav_graph)
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, navHostFragment)
                .setPrimaryNavigationFragment(navHostFragment)
                .commitNow()
        }

        // Get NavController
        val navController = navHostFragment.navController

        // Setup BottomNavigationView with NavController
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)


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

        // Set the item selected listener for BottomNavigationView
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

    private fun loadFragment(fragment: Fragment, tag: String, firstName: String?, middleName: String?, lastName: String?, userName: String?, userAddress: String?, email: String?, profileImageUrl: String?, additionalParams: Bundle? = null) {
        val bundle = Bundle().apply {
            putString("USER_NAME", userName)
            putString("FIRST_NAME", firstName)
            putString("MIDDLE_NAME", middleName)
            putString("LAST_NAME", lastName)
            putString("USER_ADDRESS", userAddress)
            putString("EMAIL", email)
            putString("PROFILE_IMAGE_URL", profileImageUrl)
            additionalParams?.let { putAll(it) }
        }

        fragment.arguments = bundle

        // Start a new transaction to replace the current fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment, tag) // Replace fragment in the container
        transaction.addToBackStack(null) // Add transaction to back stack for navigation
        transaction.commit() // Commit the transaction
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionHelper.destroy() // Clean up resources
    }
}

