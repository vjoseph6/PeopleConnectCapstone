//PART OF NOTIFICATION
package com.capstone.peopleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.capstone.peopleconnect.Client.C1WelcomeClient
import com.capstone.peopleconnect.SPrvoider.SP1WelcomeSProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Device
import io.getstream.chat.android.models.PushProvider
import io.getstream.chat.android.models.User as ChatUser

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

    companion object {
        fun registerDeviceForPushNotifications(currentUser: FirebaseUser, context: Context) {
            val client = ChatClient.instance()
            val user = currentUser.photoUrl?.toString()?.let {
                ChatUser(
                    id = currentUser.uid,
                    name = currentUser.displayName ?: "User",
                    image = it
                )
            }

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result

                    // Create device
                    val device = Device(
                        token = token,
                        pushProvider = PushProvider.FIREBASE,
                        providerName = "firebase"
                    )

                    // Register device with Stream
                    client.addDevice(device)

                    // Store in Firebase
                    val database = FirebaseDatabase.getInstance()
                    val userRef = database.getReference("users").child(currentUser.uid)
                    userRef.child("fcmToken").setValue(token)

                    Log.d("PushNotification", "Device registered successfully")
                } else {
                    Log.e("PushNotification", "Failed to get FCM token", task.exception)
                }
            }
        }
    }
}