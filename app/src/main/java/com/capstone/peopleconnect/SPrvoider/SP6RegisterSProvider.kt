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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class SP6RegisterSProvider : AppCompatActivity() {
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
        setContentView(R.layout.activity_sp6_register_sprovider)

        auth = FirebaseAuth.getInstance()
        userRole = "Service Provider"

        val toSignIn = findViewById<TextView>(R.id.signupLink)
        toSignIn.setOnClickListener {
            val intent = Intent(this, SP5LoginSProvider::class.java)
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
        val passwordEt = findViewById<EditText>(R.id.password)
        val confirmPasswordEt = findViewById<EditText>(R.id.confirmpassword)
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
                        val address = addresses[0].getAddressLine(0)
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
        val pass = findViewById<EditText>(R.id.password).text.toString().trim()
        val confirmPass = findViewById<EditText>(R.id.confirmpassword).text.toString().trim()
        val address = addressEt.text.toString().trim()
        val name = "$fName $mName $lName"

        if (fName.isEmpty() || mName.isEmpty() || lName.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty() || address.isEmpty()) {
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

        // Check if email already exists
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User created successfully, now save user data in the Realtime Database
                    saveUserData(name, fName, mName, lName, email, pass, address)
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        // Email already exists, check for existing role and prompt the user
                        checkExistingRoleAndPrompt(name, email, pass, fName, mName, lName, address)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Failed to register: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@SP6RegisterSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun checkExistingRoleAndPrompt(name: String, email: String, pass: String, fName: String, mName: String, lName: String, address: String) {
        progressBar.visibility = View.VISIBLE
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var roleMismatch = false
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        val userRoles = user?.roles ?: listOf()
                        if (!userRoles.contains(userRole)) {
                            roleMismatch = true
                            break
                        }
                    }
                    if (roleMismatch) {
                        // Show a dialog to the user to unify accounts
                        showUnifyAccountDialog(name, email, pass, fName, mName, lName, address)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@SP6RegisterSProvider, "Email already registered with the same role.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Email does not exist in the database but exists in Auth (which is rare but possible)
                    saveUserData(name, fName, mName, lName, email, pass, address)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SP6RegisterSProvider, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUnifyAccountDialog(name: String, email: String, pass: String, fName: String, mName: String, lName: String, address: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Account Already Exists")
        builder.setMessage("An account with this email already exists with a different role. Would you like to unify your roles under the same account?")
        builder.setPositiveButton("Yes") { _, _ ->
            // Unify roles
            unifyAccount(name, email, fName, mName, lName, address)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
            progressBar.visibility = View.GONE
        }
        builder.show()
    }

    private fun unifyAccount(name: String, email: String, fName: String, mName: String, lName: String, address: String) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val existingRoles = userSnapshot.child("roles").value as? List<String> ?: listOf()
                        val updatedRoles = existingRoles.toMutableList().apply { add(userRole) }
                        userSnapshot.ref.child("roles").setValue(updatedRoles)
                            .addOnCompleteListener { task ->
                                progressBar.visibility = View.GONE
                                if (task.isSuccessful) {
                                    Toast.makeText(this@SP6RegisterSProvider, "Role added successfully.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@SP6RegisterSProvider, SP5LoginSProvider::class.java)
                                    startActivity(intent)
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

    private fun saveUserData(name: String, fName: String, mName: String, lName: String, email: String, pass: String, address: String) {
        val uid = auth.currentUser?.uid
        userRole = "Service Provider"
        if (uid != null) {
            val user = User(
                firstName = fName,
                middleName = mName,
                lastName = lName,
                email = email,
                address = address,
                name = name,
                roles = listOf(userRole)
            )
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
        selectedImageUri?.let { imageUri ->
            val imageRef = storageReference.child("$uid.jpg")
            imageRef.putFile(imageUri).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    databaseReference.child(uid).child("profileImageUrl").setValue(imageUrl)
                        .addOnCompleteListener { task ->
                            progressBar.visibility = View.GONE
                            if (task.isSuccessful) {
                                val intent = Intent(this, SP5LoginSProvider::class.java)
                                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Failed to upload profile image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            profileImage.setImageURI(selectedImageUri)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    companion object {
        private const val GALLERY_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
    }
}
