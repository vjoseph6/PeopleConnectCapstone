package com.capstone.peopleconnect.SPrvoider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SP6RegisterSProvider : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var progressBar: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var addressEt: EditText
    private lateinit var userRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sp6_register_sprovider)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            auth.signOut()
        }

        userRole = "Service Provider"
        progressBar = findViewById(R.id.progressbar)

        val toSignIn = findViewById<TextView>(R.id.signinLink)
        toSignIn.setOnClickListener {
            startActivity(Intent(this, SP5LoginSProvider::class.java))
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        storageReference = FirebaseStorage.getInstance().reference.child("profile_images")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val emojiFilter = InputFilter { source, start, end, _, _, _ ->
            for (index in start until end) {
                val type = Character.getType(source[index].toInt())
                if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                    return@InputFilter ""
                }
            }
            null
        }

        val emailEt = findViewById<EditText>(R.id.email)
        val passEt = findViewById<EditText>(R.id.pass)
        val confirmPassEt = findViewById<EditText>(R.id.confirmPass)

        emailEt.filters = arrayOf(emojiFilter)
        passEt.filters = arrayOf(emojiFilter)
        confirmPassEt.filters = arrayOf(emojiFilter)

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val email = findViewById<EditText>(R.id.email).text.toString().trim()
        val pass = findViewById<EditText>(R.id.pass).text.toString().trim()
        val confirmPass = findViewById<EditText>(R.id.confirmPass).text.toString().trim()

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = View.VISIBLE

        checkIfEmailExistsInRealtimeDatabase(email) { emailExists, existingRoles ->
            if (emailExists) {
                progressBar.visibility = View.GONE
                if (existingRoles.contains(userRole)) {
                    // Email exists with the same role
                    Toast.makeText(this, "Account with this email and role already exists.", Toast.LENGTH_SHORT).show()
                } else {
                    // Email exists but with a different role, unify account
                    val builder = android.app.AlertDialog.Builder(this)
                    builder.setTitle("Unify Account")
                    builder.setMessage("An account with this email exists but with a different role. Do you want to unify your account by adding this role?")
                    builder.setPositiveButton("Yes") { dialog, _ ->
                        unifyAccount(email)
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
            } else {
                // No account exists, proceed to create in Firebase Authentication
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            sendVerificationEmail()  // Send verification email
                            saveCredentialsLocally(email, pass)  // Temporarily save credentials locally

                            // Get the Firebase UID for the newly registered user
                            val userId = task.result?.user?.uid

                            // Save the user data including the UID in the database
                            userId?.let {
                                saveUserData(email, pass, userId)
                            }

                        } else {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun unifyAccount(email: String) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val existingRoles = userSnapshot.child("roles").value as? List<String> ?: listOf()
                        val updatedRoles = existingRoles.toMutableList().apply { add(userRole) }  // Add the new role

                        // Update the roles in the database
                        userSnapshot.ref.child("roles").setValue(updatedRoles)
                            .addOnCompleteListener { task ->
                                progressBar.visibility = View.GONE
                                if (task.isSuccessful) {
                                    Toast.makeText(this@SP6RegisterSProvider, "Role added successfully.", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@SP6RegisterSProvider, SP5LoginSProvider::class.java))
                                } else {
                                    Toast.makeText(this@SP6RegisterSProvider, "Failed to unify account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SP6RegisterSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkIfEmailExistsInRealtimeDatabase(email: String, callback: (Boolean, List<String>) -> Unit) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var emailExists = false
                var existingRoles: List<String> = emptyList()

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user?.email == email) {
                        emailExists = true
                        existingRoles = user.roles ?: emptyList()
                        break
                    }
                }

                callback(emailExists, existingRoles)
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SP6RegisterSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(false, emptyList())
            }
        })
    }

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show()
                promptForVerificationCheck()
            } else {
                Toast.makeText(this, "Failed to send verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptForVerificationCheck() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Email Verification")
        builder.setMessage("Please verify your email and click 'Check Verification' once completed.")
        builder.setPositiveButton("Check Verification") { dialog, _ ->
            checkEmailVerification()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun checkEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            // Reload user data
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Check if the email is verified
                    if (user.isEmailVerified) {
                        // Save the user data if the email is verified
                        val email = user.email ?: return@addOnCompleteListener
                        val pass = getStoredPassword()
                        val userId = user.uid // Retrieve the UID
                        saveUserData(email, pass, userId) // Pass the userId to saveUserData
                    } else {
                        // Optionally, handle the case where the email is not verified
                        // e.g., show a message or prompt the user to verify their email
                    }
                } else {
                    // Handle the reload failure (optional)
                    Log.e("EmailVerification", "Failed to reload user: ${task.exception?.message}")
                }
            }
        } else {
            // Handle the case where no user is logged in (optional)
            Log.d("EmailVerification", "No user is currently logged in.")
        }
    }

    private fun saveCredentialsLocally(email: String, pass: String) {
        val sharedPreferences = getSharedPreferences("TempUserData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", pass)
        editor.apply()
    }

    private fun saveUserData(email: String, pass: String, userId: String) {
        val userId = auth.currentUser?.uid ?: return
        val user = User(
            email = email,
            roles = listOf(userRole),
            userId = userId  // Ensure this field is passed correctly
        )

        databaseReference.child(userId).setValue(user).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SP5LoginSProvider::class.java))
            } else {
                Toast.makeText(this, "Failed to save user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getStoredPassword(): String {
        val sharedPreferences = getSharedPreferences("TempUserData", MODE_PRIVATE)
        return sharedPreferences.getString("password", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        checkEmailVerification()
    }


}
