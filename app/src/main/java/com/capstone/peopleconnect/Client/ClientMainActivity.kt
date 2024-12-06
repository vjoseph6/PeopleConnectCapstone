package com.capstone.peopleconnect.Client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Adapters.SpeechRecognitionCallback
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient
import com.capstone.peopleconnect.Client.Fragments.CategoryFragmentClient
import com.capstone.peopleconnect.Client.Fragments.HomeFragmentClient
import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.Helper.NotificationHelper
import com.capstone.peopleconnect.Helper.SpeechRecognitionHelper
import com.capstone.peopleconnect.Helper.WitAiHandler
import com.capstone.peopleconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth

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
    private lateinit var btnToggleListening: ExtendedFloatingActionButton
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_main)

        // Add this after initializing Firebase Auth
        FirebaseAuth.getInstance().currentUser?.let { user ->
            NotificationHelper.setupActivityNotificationMonitoring(
                context = this,
                userId = user.uid
            )
        }

        // Initialize WitAI and Speech Recognition
        setupSpeechRecognition()

        // Initialize UI elements
        setupUI()

        // Get intent extras
        getIntentExtras()


        // Initialize bottom navigation
        setupBottomNavigation(savedInstanceState)
    }

    private fun setupSpeechRecognition() {
        witAiHandler = WitAiHandler(this)
        speechRecognitionHelper = SpeechRecognitionHelper(this, createSpeechCallback())
        speechRecognitionHelper.requestMicrophonePermission(this)
    }

    private fun setupUI() {
        btnToggleListening = findViewById(R.id.btnToggleListening)
        btnToggleListening.apply {
            setIconResource(R.drawable.client_mic)
            text = "default"
            shrink()
        }
        setupToggleListeningButton()
    }

    private fun getIntentExtras() {
        email = intent.getStringExtra("EMAIL") ?: ""
        firstName = intent.getStringExtra("FIRST_NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        middleName = intent.getStringExtra("MIDDLE_NAME") ?: ""
        lastName = intent.getStringExtra("LAST_NAME") ?: ""
        address = intent.getStringExtra("USER_ADDRESS") ?: ""
        profileImage = intent.getStringExtra("PROFILE_IMAGE_URL") ?: ""
    }

    private fun setupBottomNavigation(savedInstanceState: Bundle?) {
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Handle initial fragment loading
        if (savedInstanceState == null) {
            when (intent.getStringExtra("FRAGMENT_TO_LOAD")) {
                "CategoryFragmentClient" -> {
                    loadFragment(CategoryFragmentClient(), "categories", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.categories
                }
                "ActivityFragmentClient" -> {
                    loadFragment(ActivityFragmentClient(), "activities", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.activities
                }
                else -> {
                    loadFragment(HomeFragmentClient(), "home", firstName, middleName, lastName, userName, address, email, profileImage)
                    bottomNavigationView.selectedItemId = R.id.home
                }
            }
        }

        // Set up navigation item selection listener
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

    private fun setupToggleListeningButton() {
        btnToggleListening.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
    }

    private fun stopListening() {
        speechRecognitionHelper.stopSpeechToText()
        btnToggleListening.apply {
            shrink()
            setIconResource(R.drawable.client_mic)
        }
        isListening = false
    }

    private fun startListening() {
        if (::speechRecognitionHelper.isInitialized) {
            speechRecognitionHelper.startSpeechToText()
            btnToggleListening.apply {
                extend()
                text = "Listening..."
                setIconResource(R.drawable.client_mic)
            }
            isListening = true
        } else {
            Log.d("ClientMainActivity", "SpeechRecognizer not initialized yet.")
        }
    }

    private fun createSpeechCallback() = object : SpeechRecognitionCallback {
        override fun onSpeechResult(result: String) {
            witAiHandler.sendMessageToWit(result, createWitAiCallback())
        }

        override fun onError(error: String) {
            Toast.makeText(this@ClientMainActivity, "Error occurred: Stopped before recognizing speech", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createWitAiCallback() = object : WitAiHandler.WitAiCallback {
        override fun onResponse(
            bookDay: String,
            startTime: String,
            endTime: String,
            rating: String,
            serviceType: String,
            target: String,
            intent: String
        ) {
            handleWitAiResponse(
                bookDay,
                startTime,
                endTime,
                rating,
                serviceType,
                target,
                intent
            )
        }

        override fun onError(errorMessage: String) {
            Log.e("ClientMainActivity", "Wit.ai error: $errorMessage")
            runOnUiThread {
                Toast.makeText(
                    this@ClientMainActivity,
                    "Error processing command: $errorMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleWitAiResponse(
        bookDay: String,
        startTime: String,
        endTime: String,
        rating: String,
        serviceType: String,
        target: String,
        intent: String,
    ) {
        runOnUiThread {
            // First, check the intent
            when {
                intent == "cancel_booking"  -> {
                    // High confidence cancel booking intent
                    handleCancelBooking(target, serviceType, intent)
                    return@runOnUiThread
                }
                intent == "book_service"  -> {
                    // High confidence book service intent
                    handleBookService(bookDay, startTime, endTime, rating, serviceType, intent)
                    return@runOnUiThread
                }

                intent == "add_post"  -> {
                    // High confidence book service intent
                    handleAddPost(target, serviceType, intent)

                    if (target == "add_post") {
                        // High confidence add post intent
                        handleAddPost(target, serviceType, intent)
                    }

                    return@runOnUiThread
                }

                // If no recognizable intent, fall back to target
                target == "cancel_booking" -> {
                    handleCancelBooking(target, serviceType, intent)
                }

                target == "book_service" -> {
                    handleBookService(bookDay, startTime, endTime, rating, serviceType, intent)
                }
                target == "view_profile" -> {
                    handleViewProfile()
                }
                else -> {
                    Toast.makeText(
                        this,
                        "Command not recognized: $target (Intent: $intent)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleAddPost(target: String, serviceType: String, intent: String) {

        val additionalParams = Bundle().apply {
            putString("target", target)
            putString("serviceType", serviceType)
            putString("intent", intent)
        }
        loadFragment(ProfileFragmentClient(), "profile", firstName, middleName, lastName, userName, address, email, profileImage, additionalParams)
        bottomNavigationView.selectedItemId = R.id.profile
    }

    private fun handleBookService(bookDay: String, startTime: String, endTime: String, rating: String, serviceType: String, intent: String) {
        val additionalParams = Bundle().apply {
            putString("bookDay", bookDay)
            putString("startTime", startTime)
            putString("endTime", endTime)
            putString("rating", rating)
            putString("serviceType", serviceType)
            putString("target", "book_service")
            putString("intent", intent)
        }
        loadFragment(CategoryFragmentClient(), "categories", firstName, middleName, lastName, userName, address, email, profileImage, additionalParams)
        bottomNavigationView.selectedItemId = R.id.categories
    }

    private fun handleViewProfile() {
        loadFragment(ProfileFragmentClient(), "profile", firstName, middleName, lastName, userName, address, email, profileImage)
        bottomNavigationView.selectedItemId = R.id.profile
    }

    private fun handleCancelBooking(target: String, serviceType: String, intent: String) {
        val additionalParams = Bundle().apply {
            putString("target", target)
            putString("serviceType", serviceType)
            putString("intent", intent)
        }
        loadFragment(ActivityFragmentClient(), "activities", firstName, middleName, lastName, userName, address, email, profileImage, additionalParams)
        bottomNavigationView.selectedItemId = R.id.activities
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        speechRecognitionHelper.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
        if (currentFragment is CategoryFragmentClient) {
            currentFragment.handleFragmentBackPress()
        } else {
            if (backPressedOnce) {
                finishAffinity()
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment, tag)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionHelper.destroy()
    }
}