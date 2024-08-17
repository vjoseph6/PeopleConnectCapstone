package com.capstone.peopleconnect.SPrvoider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.capstone.peopleconnect.Client.C2Getstarted1Client
import com.capstone.peopleconnect.R

class SP1WelcomeSProvider : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sp1_welcome_sprovider)

        Handler().postDelayed({
            // Start the next activity after a 2-second delay
            val intent = Intent(this, SP2Getstarted1SProvider::class.java)
            startActivity(intent)
            // Optionally finish the current activity if you don't want to return to it
            finish()
        }, 1500) // 2000 milliseconds = 2 seconds

    }
}