package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.capstone.peopleconnect.R

class SP1WelcomeSProvider : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to the welcome screen layout
        setContentView(R.layout.activity_sp1_welcome_sprovider)

        // Access SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val hasSeenWelcomeScreen = sharedPreferences.getBoolean("hasSeenWelcomeScreen", false)

        if (!hasSeenWelcomeScreen) {
            // If the welcome screen hasn't been shown yet, show it and transition after a delay
            Handler().postDelayed({
                // Start SProviderViewPager after the delay
                val intent = Intent(this, SProviderViewPager::class.java)
                startActivity(intent)
                finish()

                // Update SharedPreferences to mark that the welcome screen has been shown
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putBoolean("hasSeenWelcomeScreen", true)
                editor.apply()
            }, 1500) // 1500 milliseconds = 1.5 seconds
        } else {
            // If the welcome screen has been shown, transition to SP5LoginSProvider after a delay
            Handler().postDelayed({
                val intent = Intent(this, SP5LoginSProvider::class.java)
                startActivity(intent)
                finish()
            }, 1500) // 1500 milliseconds = 1.5 seconds
        }
    }
}
