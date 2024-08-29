package com.capstone.peopleconnect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.capstone.peopleconnect.Client.C1WelcomeClient
import com.capstone.peopleconnect.SPrvoider.SP1WelcomeSProvider

class SelectAccount : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_account)

        val toCLient = findViewById<CardView>(R.id.cardClient)
        val toSProvider = findViewById<CardView>(R.id.cardServiceProvider)

        toCLient.setOnClickListener {

            val intent = Intent(this@SelectAccount, C1WelcomeClient::class.java)
            startActivity(intent)

        }

        toSProvider.setOnClickListener {

            val intent = Intent(this@SelectAccount, SP1WelcomeSProvider::class.java)
            startActivity(intent)

        }

    }
}