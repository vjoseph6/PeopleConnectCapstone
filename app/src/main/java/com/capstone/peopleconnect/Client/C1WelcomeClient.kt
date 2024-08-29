package com.capstone.peopleconnect.Client

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.SProviderViewPager
import com.capstone.peopleconnect.SPrvoider.SP5LoginSProvider

class C1WelcomeClient : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to the welcome client layout
        setContentView(R.layout.activity_c1_welcome_client)

        // Access SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("AngAppPrefs", MODE_PRIVATE)
        val hasSeenWelcomeScreen = sharedPreferences.getBoolean("hasSeenWelcomeScreenClient", false)

        if (!hasSeenWelcomeScreen) {
            // If the welcome screen hasn't been shown yet, show it and transition after a delay
            Handler().postDelayed({
                // Start C2Getstarted1Client after the delay
                val intent = Intent(this, ClientViewPager::class.java)
                startActivity(intent)
                finish()

                // Update SharedPreferences to mark that the welcome screen has been shown
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putBoolean("hasSeenWelcomeScreenClient", true)
                editor.apply()
            }, 1500) // 1500 milliseconds = 1.5 seconds
        } else {
            // If the welcome screen has been shown, transition to SP5LoginSProvider after a delay
            Handler().postDelayed({
                val intent = Intent(this, C5LoginClient::class.java)
                startActivity(intent)
                finish()
            }, 1500) // 1500 milliseconds = 1.5 seconds
        }
    }
}
