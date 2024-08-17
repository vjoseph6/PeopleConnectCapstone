package com.capstone.peopleconnect.Client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class C6RegisterClient : AppCompatActivity() {
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

        // Check if email already exists
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var roleMismatch = false
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user?.roles != userRole) {
                            roleMismatch = true
                            break
                        }
                    }
                    if (roleMismatch) {
                        // Proceed to register with the new role
                        registerNewUser(name, email, pass, address)
                    } else {
                        Toast.makeText(this@C6RegisterClient, "Email already registered with the same role.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Email does not exist; proceed with registration
                    registerNewUser(name, email, pass, address)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@C6RegisterClient, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerNewUser(name: String, email: String, pass: String, address: String) {
        progressBar.visibility = View.VISIBLE
        uploadImageToStorage(name, email, pass, address)
    }

    private fun uploadImageToStorage(name: String, email: String, pass: String, address: String) {
        selectedImageUri?.let { uri ->
            val imageRef = storageReference.child("${System.currentTimeMillis()}_${uri.lastPathSegment}")
            val uploadTask = imageRef.putFile(uri)

            // Show progress bar before starting the upload task
            progressBar.visibility = View.VISIBLE

            uploadTask.addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    val user = User(name, email, pass, address, imageUrl, userRole)
                    databaseReference.push().setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, C5LoginClient::class.java))
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to register: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                // Hide progress bar after the upload task completes (whether successful or not)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2000
    }
}
