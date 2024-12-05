//PART OF NOTIFICATION
package com.capstone.peopleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
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
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast

class SelectAccount : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var cardClient: CardView
    private lateinit var cardServiceProvider: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSelectAccountType: TextView
    private lateinit var tvSubtitle: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_account)

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        cardClient = findViewById(R.id.cardClient)
        cardServiceProvider = findViewById(R.id.cardServiceProvider)
        tvSelectAccountType = findViewById(R.id.tvSelectAccountType)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Show loading state
            showLoading(true)
            // Check user role and redirect
            checkUserRoleAndRedirect(currentUser.email ?: "")
        } else {
            // Show account selection UI
            showLoading(false)
            setupAccountSelection()
        }
    }
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        cardClient.visibility = if (isLoading) View.GONE else View.VISIBLE
        cardServiceProvider.visibility = if (isLoading) View.GONE else View.VISIBLE
        tvSelectAccountType.visibility = if (isLoading) View.GONE else View.VISIBLE
        tvSubtitle.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun setupAccountSelection() {
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
        // Add a timeout handler
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            showLoading(false)
            setupAccountSelection()
            Toast.makeText(this, "Connection timeout. Please try again.", Toast.LENGTH_SHORT).show()
        }
        timeoutHandler.postDelayed(timeoutRunnable, 10000) // 10 second timeout

        // Optimize query by adding indexOn rule and limiting to 1 result
        databaseReference.orderByChild("email").equalTo(email).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Remove the timeout handler since we got a response
                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.first()
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let {
                            when {
                                it.roles?.contains("Client") == true -> {
                                    redirectToClientMain(user)
                                }
                                it.roles?.contains("Service Provider") == true -> {
                                    redirectToProviderMain(user)
                                }
                                else -> {
                                    showLoading(false)
                                    setupAccountSelection()
                                }
                            }
                        } ?: run {
                            showLoading(false)
                            setupAccountSelection()
                        }
                    } else {
                        showLoading(false)
                        setupAccountSelection()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Remove the timeout handler
                    timeoutHandler.removeCallbacks(timeoutRunnable)

                    Log.e("SelectAccount", "Error checking user role", error.toException())
                    showLoading(false)
                    setupAccountSelection()
                    Toast.makeText(this@SelectAccount, "Error checking account type. Please try again.", Toast.LENGTH_SHORT).show()
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