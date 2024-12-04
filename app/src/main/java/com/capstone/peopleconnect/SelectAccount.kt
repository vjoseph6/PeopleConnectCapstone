//PART OF NOTIFICATION
package com.capstone.peopleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Client.C1WelcomeClient
import com.capstone.peopleconnect.Client.ClientMainActivity
import com.capstone.peopleconnect.SPrvoider.SP1WelcomeSProvider
import com.capstone.peopleconnect.SPrvoider.SProviderMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Device
import io.getstream.chat.android.models.PushProvider
import io.getstream.chat.android.models.User as ChatUser

class SelectAccount : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_account)


        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.email ?: "")
        }

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

    private fun checkUserRoleAndRedirect(email: String) {
        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            user?.let {
                                when {
                                    it.roles?.contains("Client") == true -> {
                                        redirectToClientMain(user)
                                    }
                                    it.roles?.contains("Service Provider") == true -> {
                                        redirectToProviderMain(user)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SelectAccount", "Error checking user role", error.toException())
                }
            })
    }

    private fun redirectToClientMain(user: User) {
        val intent = Intent(this, ClientMainActivity::class.java).apply {
            putExtra("USER_NAME", user.name)
            putExtra("FIRST_NAME", user.firstName)
            putExtra("MIDDLE_NAME", user.middleName)
            putExtra("LAST_NAME", user.lastName)
            putExtra("USER_ADDRESS", user.address)
            putExtra("EMAIL", user.email)
            putExtra("PROFILE_IMAGE_URL", user.profileImageUrl)
        }
        startActivity(intent)
        finish()
    }

    private fun redirectToProviderMain(user: User) {
        val intent = Intent(this, SProviderMainActivity::class.java).apply {
            putExtra("USER_NAME", user.name)
            putExtra("FIRST_NAME", user.firstName)
            putExtra("MIDDLE_NAME", user.middleName)
            putExtra("LAST_NAME", user.lastName)
            putExtra("USER_ADDRESS", user.address)
            putExtra("EMAIL", user.email)
            putExtra("PROFILE_IMAGE_URL", user.profileImageUrl)
        }
        startActivity(intent)
        finish()
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