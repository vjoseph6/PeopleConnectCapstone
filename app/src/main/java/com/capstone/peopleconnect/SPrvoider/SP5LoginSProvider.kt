package com.capstone.peopleconnect.SPrvoider

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Client.C1WelcomeClient
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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

        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPassword)
        forgotPasswordLink.setOnClickListener {
            showForgotPasswordDialog()
        }

    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val email = emailEditText.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                } else {
                    checkEmailExistsAndSendReset(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun checkEmailExistsAndSendReset(email: String) {
        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        sendPasswordResetEmail(email)
                    } else {
                        Toast.makeText(this@SP5LoginSProvider, "Email does not exist!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SP5LoginSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendPasswordResetEmail(email: String) {
        Log.d("PasswordReset", "Attempting to send password reset email to: $email")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PasswordReset", "Password reset email sent successfully to: $email")
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                } else {
                    handlePasswordResetError(task.exception)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PasswordReset", "Error occurred: ${e.localizedMessage}")
                Toast.makeText(this, "Error occurred: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handlePasswordResetError(exception: Exception?) {
        val exceptionMessage = exception?.localizedMessage
        Log.e("PasswordReset", "Failed to send reset email: $exceptionMessage")
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                Toast.makeText(
                    this,
                    "No account found with this email.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(
                    this,
                    "Invalid email format.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Failed to send reset email: $exceptionMessage",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun signInUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate email and password input
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password cannot be empty"
            return
        }

        // Sign in using Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Check user roles
                    checkUserRoles(email)
                } else {
                    // Sign in failed, handle the error
                    handleSignInError(task.exception)
                }
            }
    }

    private fun checkUserRoles(email: String) {
        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var isServiceProvider = false
                        var userName = ""
                        var userAddress = ""
                        var profileImageUrl = ""
                        var fName = ""
                        var mName = ""
                        var lName = ""

                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            val userRoles = user?.roles ?: listOf()
                            if (userRoles.contains("Service Provider")) {
                                isServiceProvider = true
                                userName = user?.name ?: ""
                                fName = user?.firstName ?: ""
                                mName = user?.middleName ?: ""
                                lName = user?.lastName ?: ""
                                userAddress = user?.address ?: ""
                                profileImageUrl = user?.profileImageUrl ?: ""
                                break
                            }
                        }

                        if (isServiceProvider) {

                            // Save current user details in shared preferences
                            saveCurrentUser(email, userName, userAddress, profileImageUrl)

                            // Pass user details to the next activity
                            val intent = Intent(this@SP5LoginSProvider, SProviderMainActivity::class.java).apply {
                                putExtra("USER_NAME", userName)
                                putExtra("FIRST_NAME", fName)
                                putExtra("MIDDLE_NAME", mName)
                                putExtra("LAST_NAME", lName)
                                putExtra("USER_ADDRESS", userAddress)
                                putExtra("EMAIL", email)
                                putExtra("PROFILE_IMAGE_URL", profileImageUrl)
                            }
                            Toast.makeText(this@SP5LoginSProvider, "Welcome Service Provider $fName", Toast.LENGTH_SHORT).show()
                            startActivity(intent)
                        } else {
                            // User is not a Service Provider
                            Toast.makeText(this@SP5LoginSProvider, "User does not exist", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Email does not exist in the database
                        Toast.makeText(this@SP5LoginSProvider, "No account found with this email.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SP5LoginSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveCurrentUser(email: String, firstName: String, address: String, profileImageUrl: String) {
        // Save the current user's details in shared preferences
        val sharedPref = getSharedPreferences("com.capstone.peopleconnect.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("currentEmail", email)
            putString("currentFirstName", firstName)
            putString("currentAddress", address)
            putString("currentProfileImageUrl", profileImageUrl)
            apply()
        }
    }



    private fun handleSignInError(exception: Exception?) {
        val exceptionMessage = exception?.localizedMessage
        Log.e("SignInError", "Failed to sign in: $exceptionMessage")
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Sign-in failed: $exceptionMessage", Toast.LENGTH_SHORT).show()
            }
        }
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