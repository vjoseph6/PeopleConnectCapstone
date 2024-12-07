package com.capstone.peopleconnect.Client

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
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.SProviderMainActivity
import com.capstone.peopleconnect.SelectAccount
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
        // Inflate the custom layout for forgot password dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)

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

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Reload user to get latest verification status
                        user.reload().addOnCompleteListener { reloadTask ->
                            if (reloadTask.isSuccessful) {
                                if (user.isEmailVerified) {
                                    // Email is verified, check user status and role
                                    checkUserInDatabase(email)
                                } else {
                                    // Email not verified, sign out and show dialog
                                    auth.signOut()
                                    showVerificationDialog()
                                }
                            }
                        }
                    }
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
                    var isClient = false
                    var userName = ""
                    var userAddress = ""
                    var profileImageUrl = ""
                    var fName = ""
                    var mName = ""
                    var lName = ""
                    var userStatus = ""

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
                            userStatus = user?.status ?: ""
                            break
                        }
                    }

                    if (isClient) {
                        if (userStatus == "enabled") {
                            // User is verified and enabled, proceed with login
                            handleSuccessfulLogin(email, userName, userAddress, profileImageUrl, fName, mName, lName)
                        } else {
                            auth.signOut()
                            Toast.makeText(this@C5LoginClient, "Your account has been disabled. Please contact support.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        auth.signOut()
                        Toast.makeText(this@C5LoginClient, "User does not exist", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    auth.signOut()
                    Toast.makeText(this@C5LoginClient, "No account found with this email.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                auth.signOut()
                Toast.makeText(this@C5LoginClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleSuccessfulLogin(email: String, userName: String, userAddress: String, profileImageUrl: String, 
                                    fName: String, mName: String, lName: String) {
        // Register device for push notifications
        FirebaseAuth.getInstance().currentUser?.let { user ->
            SelectAccount.registerDeviceForPushNotifications(user, this@C5LoginClient)
        }

        // Save current user details
        saveCurrentUser(email, userName, userAddress, profileImageUrl)

        // Navigate to next activity
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
        finish()
    }

    private fun showVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
            .setMessage("Please verify your email by clicking the verification link sent to your email address.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Check verification status when returning to the activity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (currentUser.isEmailVerified) {
                        // If verified, proceed with login
                        checkUserInDatabase(currentUser.email ?: "")
                    }
                }
            }
        }
    }

    private fun handleSignInError(exception: Exception?) {
        val exceptionMessage = exception?.localizedMessage
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                if (exception.message?.contains("The user account has been disabled by an administrator") == true) {
                    Toast.makeText(
                        this,
                        "Your account has been disabled by the admin.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show()
                }
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
