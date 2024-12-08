package com.capstone.peopleconnect.SPrvoider

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
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
import com.capstone.peopleconnect.SelectAccount
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
        // Inflate the custom layout for forgot password dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password_sprovider, null)

        // Create an AlertDialog and apply customizations
        val builder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)  // Prevent dialog from closing if the background is touched
        }

        val dialog = builder.create()

        // Make the background transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        // Apply window animations (ensure you have the animation style defined in your styles.xml)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Show the dialog
        dialog.show()

        // Set up the 'Submit' button action
        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val email = dialogView.findViewById<EditText>(R.id.emailEditText).text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                // Call the method to check if the email exists and send the password reset email
                checkEmailExistsAndSendReset(email)
                dialog.dismiss()  // Close the dialog after submitting
            }
        }

        // Set up the 'Cancel' text action
        dialogView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()  // Close the dialog when cancel is clicked
        }
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

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Directly check user in database without email verification
                    checkUserInDatabase(email)
                } else {
                    handleSignInError(task.exception)
                }
            }
    }

    private fun checkUserInDatabase(email: String) {
        val query = databaseReference.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null) {
                            val userRoles = user.roles ?: listOf()
                            if (userRoles.contains("Service Provider")) {
                                if (user.status == "enabled") {
                                    // User is enabled, proceed with login
                                    handleSuccessfulLogin(
                                        email,
                                        user.name ?: "",
                                        user.address ?: "",
                                        user.profileImageUrl ?: "",
                                        user.firstName ?: "",
                                        user.middleName ?: "",
                                        user.lastName ?: ""
                                    )
                                } else {
                                    auth.signOut()
                                    Toast.makeText(this@SP5LoginSProvider, "Your account has been disabled. Please contact support.", Toast.LENGTH_LONG).show()
                                }
                                return
                            }
                        }
                    }
                    // If we get here, user exists but is not a Service Provider
                    auth.signOut()
                    Toast.makeText(this@SP5LoginSProvider, "This account is not registered as a Service Provider", Toast.LENGTH_SHORT).show()
                } else {
                    auth.signOut()
                    Toast.makeText(this@SP5LoginSProvider, "No account found with this email.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                auth.signOut()
                Toast.makeText(this@SP5LoginSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccessfulLogin(email: String, username: String, address: String, profileImageUrl: String, firstName: String, middleName: String, lastName: String) {
        // Save current user details in shared preferences
        val sharedPref = getSharedPreferences("com.capstone.peopleconnect.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("currentEmail", email)
            putString("currentFirstName", firstName)
            putString("currentAddress", address)
            putString("currentProfileImageUrl", profileImageUrl)
            apply()
        }

        // Pass user details to the next activity
        val intent = Intent(this@SP5LoginSProvider, SProviderMainActivity::class.java).apply {
            putExtra("USER_NAME", username)
            putExtra("FIRST_NAME", firstName)
            putExtra("MIDDLE_NAME", middleName)
            putExtra("LAST_NAME", lastName)
            putExtra("USER_ADDRESS", address)
            putExtra("EMAIL", email)
            putExtra("PROFILE_IMAGE_URL", profileImageUrl)
        }
        Toast.makeText(this@SP5LoginSProvider, "Welcome Service Provider $firstName", Toast.LENGTH_SHORT).show()
        startActivity(intent)
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
