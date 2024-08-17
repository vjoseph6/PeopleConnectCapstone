package com.capstone.peopleconnect.Client

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.capstone.peopleconnect.R

class C1WelcomeClient : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c1_welcome_client)

        Handler().postDelayed({
            // Start the next activity after a 2-second delay
            val intent = Intent(this, C2Getstarted1Client::class.java)
            startActivity(intent)
            // Optionally finish the current activity if you don't want to return to it
            finish()
        }, 1500) // 2000 milliseconds = 2 seconds
    }
}