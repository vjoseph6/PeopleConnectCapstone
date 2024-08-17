package com.capstone.peopleconnect.SPrvoider

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Client.C1WelcomeClient
import com.capstone.peopleconnect.Client.C6RegisterClient
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SP5LoginSProvider : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sp5_login_sprovider)
        val toSignUp = findViewById<TextView>(R.id.signupLink)
        toSignUp.setOnClickListener {
            val intent = Intent(this, SP6RegisterSProvider::class.java)
            startActivity(intent)
        }

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.pass)
        val loginBtn = findViewById<Button>(R.id.loginButton)

        // Apply InputFilter to disallow emojis in input fields
        val emojiFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (index in start until end) {
                val type = Character.getType(source[index].toInt())
                if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                    return@InputFilter ""
                }
            }
            null
        }
        emailEditText.filters = arrayOf(emojiFilter)
        passwordEditText.filters = arrayOf(emojiFilter)

        loginBtn.setOnClickListener {
            signInUser()
        }
    }

    private fun signInUser() {
        val username = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || username.length < 3) {
            emailEditText.error = "Username length should be at least 3 characters"
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password cannot be empty"
            return
        }

        val userRef = databaseReference.orderByChild("email").equalTo(username)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user?.password == password) {
                            // Save the username globally
                            saveCurrentUser(username)
                            startActivity(Intent(this@SP5LoginSProvider, SP1WelcomeSProvider::class.java))
                            return
                        }
                    }
                    Toast.makeText(this@SP5LoginSProvider, "Invalid password, Please try again", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SP5LoginSProvider, "User does not exist!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SP5LoginSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveCurrentUser(username: String) {
        // Save the current user's username in shared preferences
        val sharedPref = getSharedPreferences("com.capstone.peopleconnect.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("currentUsername", username)
            apply()
        }
    }
}