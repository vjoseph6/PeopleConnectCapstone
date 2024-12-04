package com.capstone.peopleconnect.SPrvoider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.capstone.peopleconnect.Adapters.SpeechRecognitionCallback
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient
import com.capstone.peopleconnect.Client.Fragments.CategoryFragmentClient
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient

import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.Helper.SpeechRecognitionHelper
import com.capstone.peopleconnect.Helper.WitAiHandler

import com.capstone.peopleconnect.Helper.NotificationHelper

import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SProvider.Fragments.ProfileFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.ActivityFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.HomeFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.MicFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.SkillsFragmentSProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

import com.google.firebase.auth.FirebaseAuth


class SProviderMainActivity : AppCompatActivity() {

    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var userName: String
    private lateinit var middleName: String
    private lateinit var lastName: String
    private lateinit var address: String
    private lateinit var profileImage: String
    private lateinit var speechRecognitionHelper: SpeechRecognitionHelper
    private lateinit var witAiHandler: WitAiHandler
    private lateinit var btnToggleListening : ExtendedFloatingActionButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isListening = false
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprovider_main)



        witAiHandler = WitAiHandler(this@SProviderMainActivity)

        // Initialize the speech recognition helper with a callback to handle speech results
        speechRecognitionHelper = SpeechRecognitionHelper(this, object : SpeechRecognitionCallback {
            override fun onSpeechResult(result: String) {
                witAiHandler.sendMessageToWit(result, object : WitAiHandler.WitAiCallback {
                    override fun onResponse(
                        bookDay: String,
                        startTime: String,
                        endTime: String,
                        rating: String,
                        serviceType: String,
                        target: String,
                        intent: String
                    ) {
                        runOnUiThread {
                            // First, check the intent
                            when {
                                intent == "cancel_booking"  -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", "cancel_booking")
                                        putString("serviceType", serviceType)
                                        putString("intent", intent)
                                    }
                                    loadFragment(
                                        ActivityFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.activities
                                }
                                intent == "on_service"  -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", "on_service")
                                        putString("serviceType", serviceType)
                                        putString("intent", intent)
                                    }
                                    loadFragment(
                                        SkillsFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.skills
                                }
                                intent == "off_service" -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", "off_service")
                                        putString("serviceType", serviceType)
                                        putString("intent", intent)
                                    }
                                    loadFragment(
                                        SkillsFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.skills
                                }
                                // Fall back to target if intent doesn't match
                                target == "cancel_booking" -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", target)
                                        putString("serviceType", serviceType)
                                    }
                                    loadFragment(
                                        ActivityFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.activities
                                }
                                target == "on_service" -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", target)
                                        putString("serviceType", serviceType)
                                    }
                                    loadFragment(
                                        SkillsFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.skills
                                }
                                target == "off_service" -> {
                                    val additionalParams = Bundle().apply {
                                        putString("target", target)
                                        putString("serviceType", serviceType)
                                    }
                                    loadFragment(
                                        SkillsFragmentSProvider(),
                                        "activities",
                                        firstName,
                                        middleName,
                                        lastName,
                                        userName,
                                        address,
                                        email,
                                        profileImage,
                                        additionalParams
                                    )
                                    bottomNavigationView.selectedItemId = R.id.skills
                                }
                                else -> {
                                    Toast.makeText(
                                        this@SProviderMainActivity,
                                        "Command not recognized: $target",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }


                    override fun onError(errorMessage: String) {
                        Log.e("Sprovider", "Wit.ai error: $errorMessage")
                        runOnUiThread {
                            Toast.makeText(
                                this@SProviderMainActivity,
                                "Error processing command: $errorMessage",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            }

            override fun onError(error: String) {
                Toast.makeText(this@SProviderMainActivity, "Stopped before recognizing speech", Toast.LENGTH_SHORT).show()
            }
        })

        // Add this after setContentView
        FirebaseAuth.getInstance().currentUser?.let { user ->
            NotificationHelper.setupActivityNotificationMonitoring(
                context = this,
                userId = user.uid
            )
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
            navHostFragment = NavHostFragment.create(R.navigation.nav_graph_2)
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, navHostFragment)
                .setPrimaryNavigationFragment(navHostFragment)
                .commitNow()
        }

        // Get NavController
        val navController = navHostFragment.navController

        // Request microphone permission if not already granted
        speechRecognitionHelper.requestMicrophonePermission(this)

        // Setup BottomNavigationView with NavController
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        btnToggleListening = findViewById(R.id.fab_mic)

        btnToggleListening.apply {
            setIconResource(R.drawable.client_mic)
            text = "default"
            shrink()
        }

        var isListening = false

        btnToggleListening.setOnClickListener {

            if (isListening) {
                // Stop listening and shrink the button
                speechRecognitionHelper.stopSpeechToText()
                btnToggleListening.apply {
                    shrink()
                    setIconResource(R.drawable.client_mic) // Set microphone icon
                }
                isListening = false // Update the state
            } else {
                if (::speechRecognitionHelper.isInitialized) {
                    // Start listening and extend the button
                    speechRecognitionHelper.startSpeechToText()
                    btnToggleListening.apply {
                        extend()
                        text = "Listening..." // Add text
                        setIconResource(R.drawable.client_mic) // Keep microphone icon
                    }
                    isListening = true // Update the state
                } else {
                    Log.d("ClientMainActivity", "SpeechRecognizer not initialized yet.")
                }
            }
        }


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



    private fun loadFragment(fragment: Fragment, tag: String, firstName: String?, middleName: String?, lastName: String?, userName: String?, userAddress: String?, email: String?, profileImageUrl: String?, additionalParams: Bundle? = null) {
        val bundle = Bundle().apply {
            putString("USER_NAME", userName)
            putString("FIRST_NAME", firstName)
            putString("MIDDLE_NAME", middleName)
            putString("LAST_NAME", lastName)
            putString("USER_ADDRESS", userAddress)
            putString("EMAIL", email)
            putString("PROFILE_IMAGE_URL", profileImageUrl)  // Ensure profile image is passed here
            additionalParams?.let { putAll(it) }
        }
        fragment.arguments = bundle

        // Start a new transaction to replace the current fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment, tag) // Replace fragment in the container
        transaction.addToBackStack(null) // Add transaction to back stack for navigation
        transaction.commit()
    }
    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionHelper.destroy() // Clean up resources
    }

}
