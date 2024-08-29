package com.capstone.peopleconnect.Client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class C6RegisterClient : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var profileImage: ShapeableImageView
    private var selectedImageUri: Uri? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var addressEt: EditText
    private lateinit var userRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_c6_register_client)

        auth = FirebaseAuth.getInstance()
        userRole = "Client"

        val toSignIn = findViewById<TextView>(R.id.signinLink)
        toSignIn.setOnClickListener {
            val intent = Intent(this, C5LoginClient::class.java)
            startActivity(intent)
        }

        val backBtn = findViewById<ImageButton>(R.id.backButton)
        backBtn.setOnClickListener {
            onBackPressed()
        }

        // Initialize Firebase References
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        storageReference = FirebaseStorage.getInstance().reference.child("profile_images")

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Custom InputFilter to disallow emojis and show a toast message
        val emojiFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (index in start until end) {
                val type = Character.getType(source[index].toInt())
                if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                    return@InputFilter ""
                }
            }
            null
        }

        val fNameEt = findViewById<EditText>(R.id.fName)
        val mNameEt = findViewById<EditText>(R.id.mName)
        val lNameEt = findViewById<EditText>(R.id.lName)
        val emailEt = findViewById<EditText>(R.id.email)
        val passwordEt = findViewById<EditText>(R.id.pass)
        val confirmPasswordEt = findViewById<EditText>(R.id.confirmpass)
        addressEt = findViewById(R.id.address)

        fNameEt.filters = arrayOf(emojiFilter)
        mNameEt.filters = arrayOf(emojiFilter)
        lNameEt.filters = arrayOf(emojiFilter)
        emailEt.filters = arrayOf(emojiFilter)
        passwordEt.filters = arrayOf(emojiFilter)
        confirmPasswordEt.filters = arrayOf(emojiFilter)
        addressEt.filters = arrayOf(emojiFilter)

        profileImage = findViewById(R.id.profile)
        progressBar = findViewById(R.id.progressbar)

        profileImage.setOnClickListener {
            openGalleryForImage()
        }

        val locationBtn = findViewById<ImageView>(R.id.locationBtn)
        locationBtn.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        val regBtn = findViewById<Button>(R.id.registerButton)
        regBtn.setOnClickListener {
            registerUser()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses: MutableList<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses != null) {
                    if (addresses.isNotEmpty()) {
                        val address = addresses?.get(0)?.getAddressLine(0)
                        addressEt.setText(address)
                    }
                }
            } else {
                Toast.makeText(this, "Unable to get location. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser() {
        val fName = findViewById<EditText>(R.id.fName).text.toString().trim()
        val mName = findViewById<EditText>(R.id.mName).text.toString().trim()
        val lName = findViewById<EditText>(R.id.lName).text.toString().trim()
        val email = findViewById<EditText>(R.id.email).text.toString().trim()
        val pass = findViewById<EditText>(R.id.pass).text.toString().trim()
        val confirmPass = findViewById<EditText>(R.id.confirmpass).text.toString().trim()
        val address = addressEt.text.toString().trim()
        val name = "$fName $mName $lName"

        if (fName.isEmpty() || mName.isEmpty() || lName.isEmpty() || name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a profile image.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Check if an account with the same email and role already exists
        checkExistingAccount(email) { exists ->
            if (exists) {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "An account with this email and role already exists.", Toast.LENGTH_SHORT).show()
            } else {
                // Proceed with user registration
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // User created successfully, now save user data in the Realtime Database
                            saveUserData(fName, mName, lName, name, email, pass, address)
                        } else {
                            if (task.exception is FirebaseAuthUserCollisionException) {
                                // Handle case where email is already used but maybe with a different role
                                checkExistingRoleAndPrompt(fName, mName, lName, name, email, pass, address)
                            } else {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Failed to register: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }
    }

    private fun checkExistingAccount(email: String, callback: (Boolean) -> Unit) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var roleExists = false
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    val userRoles = user?.roles ?: listOf()
                    if (userRoles.contains(userRole)) {
                        roleExists = true
                        break
                    }
                }
                callback(roleExists)
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@C6RegisterClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }



    private fun checkExistingRoleAndPrompt(fName: String, mName: String, lName: String, name: String, email: String, password: String, address: String){
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var roleMismatch = false
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        // Check if the userRoles list contains the userRole
                        val userRoles = user?.roles ?: listOf()
                        if (!userRoles.contains(userRole)) {
                            roleMismatch = true
                            break
                        }
                    }
                    if (roleMismatch) {
                        // Show a dialog to the user to unify accounts
                        showUnifyAccountDialog(email, password, name, address)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@C6RegisterClient, "Email already registered with the same role.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Email does not exist in the database but exists in Auth (which is rare but possible)
                    saveUserData(fName, mName, lName, name, email, password, address)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@C6RegisterClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun showUnifyAccountDialog(email: String, pass: String, name: String, address: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Account Already Exists")
        builder.setMessage("An account with this email already exists with a different role. Would you like to unify your roles under the same account?")
        builder.setPositiveButton("Yes") { _, _ ->
            // Unify roles
            unifyAccount(email, name, address)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
            progressBar.visibility = View.GONE
        }
        builder.show()
    }

    private fun unifyAccount(email: String, name: String, address: String) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val existingRoles = userSnapshot.child("roles").value as? List<String> ?: listOf()
                        val updatedRoles = existingRoles.toMutableList().apply { add(userRole) }
                        userSnapshot.ref.child("roles").setValue(updatedRoles)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@C6RegisterClient, "Role added successfully.", Toast.LENGTH_SHORT).show()
                                    progressBar.visibility = View.GONE
                                    val intent = Intent(this@C6RegisterClient, C5LoginClient::class.java)
                                    startActivity(intent)
                                } else {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this@C6RegisterClient, "Failed to unify account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@C6RegisterClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserData(fName: String, mName: String, lName: String, name: String, email: String, password: String, address: String) {
        val uid = auth.currentUser?.uid
        userRole = "Client"
        if (uid != null) {
            Log.d("Value of User Role","$userRole")
            val user = User(firstName = fName, middleName = mName, lastName = lName, name = name, email = email, address = address, roles = listOf(userRole))
            databaseReference.child(uid).setValue(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        uploadProfileImage(uid)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Failed to save user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


    private fun uploadProfileImage(uid: String) {
        selectedImageUri?.let { uri ->
            val profileImageRef = storageReference.child("$uid.jpg")
            profileImageRef.putFile(uri)
                .addOnSuccessListener {
                    profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        databaseReference.child(uid).child("profileImageUrl").setValue(downloadUri.toString())
                            .addOnCompleteListener { task ->
                                progressBar.visibility = View.GONE
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Registration Successful.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, C5LoginClient::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to upload profile image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to upload profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            200
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 100) {
            selectedImageUri = data?.data
            profileImage.setImageURI(selectedImageUri)
        }
    }
}
