package com.capstone.peopleconnect.Client

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.capstone.peopleconnect.R

class C2Getstarted1Client : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c2_getstarted1_client)

        val next = findViewById<Button>(R.id.getstarted1_button_client)
        next.setOnClickListener {

            val intent = Intent(this, C3Getstarted2Client::class.java)
            startActivity(intent)
        }

        val skip = findViewById<TextView>(R.id.skip)
        skip.setOnClickListener {

            val intent = Intent(this, C5LoginClient::class.java)
            startActivity(intent)
        }
    }
}