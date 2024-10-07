package com.capstone.peopleconnect.Client

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
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.SProviderMainActivity
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale


class C5LoginClient : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c5_login_client)

        auth = FirebaseAuth.getInstance()

        // Set a default language code, e.g., "en" for English
        auth.setLanguageCode("en")

        // Optional: Dynamically set the language code based on the device's locale
        val locale = Locale.getDefault().language
        if (locale != null) {
            auth.setLanguageCode(locale)
        }



        val toSignUp = findViewById<TextView>(R.id.signupLink)
        toSignUp.setOnClickListener {
            startActivity(Intent(this, C6RegisterClient::class.java))
        }

        // Forgot password functionality
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)
        forgotPasswordLink.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.pass)
        val loginBtn = findViewById<Button>(R.id.loginButton)

        // Apply InputFilter to disallow emojis in input fields
        val emojiFilter = InputFilter { source, start, end, _, _, _ ->
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

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password_client, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val submitButton = dialogView.findViewById<Button>(R.id.btnSubmit)
        val cancelText = dialogView.findViewById<TextView>(R.id.tvCancel)

        submitButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                checkEmailExistsAndSendReset(email)
                dialog.dismiss()  // Dismiss the dialog after submitting
            }
        }

        cancelText.setOnClickListener {
            dialog.dismiss()  // Dismiss the dialog when Cancel is clicked
        }

        dialog.show()
    }


    private fun checkEmailExistsAndSendReset(email: String) {
        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        sendPasswordResetEmail(email)
                    } else {
                        Toast.makeText(this@C5LoginClient, "Email does not exist!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@C5LoginClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                    // Sign in successful, navigate to the next screen
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
                        var isClient = false
                        var userName = ""
                        var userAddress = ""
                        var profileImageUrl = ""
                        var fName = ""
                        var mName = ""
                        var lName = ""

                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            val userRoles = user?.roles ?: listOf()
                            if (userRoles.contains("Client")) {
                                isClient = true
                                userName = user?.name ?: ""
                                fName = user?.firstName ?: ""
                                mName = user?.middleName ?: ""
                                lName = user?.lastName ?: ""
                                userAddress = user?.address ?: ""
                                profileImageUrl = user?.profileImageUrl ?: ""
                                break
                            }
                        }

                        if (isClient) {

                            // Save current user details in shared preferences
                            saveCurrentUser(email, userName, userAddress, profileImageUrl)

                            // Pass user details to the next activity
                            val intent = Intent(this@C5LoginClient, C7ChoosingInterestClient::class.java).apply {
                                putExtra("USER_NAME", userName)
                                putExtra("FIRST_NAME", fName)
                                putExtra("MIDDLE_NAME", mName)
                                putExtra("LAST_NAME", lName)
                                putExtra("USER_ADDRESS", userAddress)
                                putExtra("EMAIL", email)
                                putExtra("PROFILE_IMAGE_URL", profileImageUrl)
                            }
                            Toast.makeText(this@C5LoginClient, "Welcome Client $fName", Toast.LENGTH_SHORT).show()
                            startActivity(intent)
                        } else {
                            // User is not a Service Provider
                            Toast.makeText(this@C5LoginClient, "User does not exist", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Email does not exist in the database
                        Toast.makeText(this@C5LoginClient, "No account found with this email.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@C5LoginClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

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
}
