package com.capstone.peopleconnect.Client.Fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.util.Locale
import android.Manifest
import android.widget.ProgressBar
import com.google.android.gms.location.LocationServices

class MyProfileFragmentClient : Fragment() {

    private var userAddress: String? = null
    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null
    private var email: String? = null
    private var profileImageUrl: String? = null
    private lateinit var locationBtn: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var addressEditText: EditText
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var userKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        arguments?.let {
            firstName = it.getString("FIRST_NAME")
            middleName = it.getString("MIDDLE_NAME")
            lastName = it.getString("LAST_NAME")
            email = it.getString("EMAIL")
            profileImageUrl = it.getString("PROFILE_IMAGE_URL")
            userAddress = it.getString("ADDRESS")
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        storageReference = FirebaseStorage.getInstance().reference.child("profile_images")

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    view?.findViewById<ImageView>(R.id.profilePicture)?.setImageURI(uri)
                }
            }
        }

        // Fetch the user key on creation
        fetchUserKey()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_profile_client, container, false)

        val backButton: ImageView = view.findViewById(R.id.btnBackClient)
        val profilePicture: ShapeableImageView = view.findViewById(R.id.profilePicture)
        val editIcon: ImageView = view.findViewById(R.id.editIcon)
        val etFirstName: EditText = view.findViewById(R.id.etFirstName)
        val etMiddleName: EditText = view.findViewById(R.id.etMiddleName)
        val etLastName: EditText = view.findViewById(R.id.etLastName)
        val etAddress: EditText = view.findViewById(R.id.address)
        val btnSave: Button = view.findViewById(R.id.btnSave)

        etFirstName.setText(firstName)
        etMiddleName.setText(middleName)
        etAddress.setText(userAddress)
        etLastName.setText(lastName)

        locationBtn = view.findViewById(R.id.locationBtn)
        addressEditText = view.findViewById(R.id.address)
        progressBar = view.findViewById(R.id.progressbar)

        locationBtn.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        if (!profileImageUrl.isNullOrEmpty()) {
            // If the URL is valid, load the image
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile)  // Placeholder image
                .error(R.drawable.profile)        // Error image
                .into(profilePicture)
        } else {
            // If profileImageUrl is empty or null, set a placeholder image
            profilePicture.setImageResource(R.drawable.profile)  // Placeholder image
        }

        editIcon.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveProfile() }
        backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        return view
    }

    //setting up address
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), // Use requireContext() instead of this
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(), // Use requireActivity() instead of this
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

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), // Use requireContext() instead of this
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault()) // Use requireContext()
                val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    addressEditText.setText(address) // Correctly set the address in the EditText
                }
            } else {
                Toast.makeText(requireContext(), "Unable to get location. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun saveProfile() {
        // Show progress bar while saving
        progressBar.visibility = View.VISIBLE

        val updatedFirstName = view?.findViewById<EditText>(R.id.etFirstName)?.text.toString()
        val updatedMiddleName = view?.findViewById<EditText>(R.id.etMiddleName)?.text.toString()
        val updatedLastName = view?.findViewById<EditText>(R.id.etLastName)?.text.toString()
        val addressText = view?.findViewById<EditText>(R.id.address)?.text.toString()

        val updatedUser = User(
            firstName = updatedFirstName,
            middleName = updatedMiddleName,
            lastName = updatedLastName,
            name = "$updatedFirstName $updatedMiddleName $updatedLastName",
            profileImageUrl = profileImageUrl ?: "",
            email = email.toString(),
            address = addressText // Include the address here
        )

        if (selectedImageUri != null) {
            val fileReference = storageReference.child("$email.jpg")
            handleImageUpload(fileReference, updatedUser)
        } else {
            updateUser(updatedUser)
        }
    }


    private fun handleImageUpload(fileReference: StorageReference, updatedUser: User) {
        // Check if the current profile image URL is empty
        if (profileImageUrl.isNullOrEmpty()) {
            // If there's no old image, directly upload the new image
            uploadNewImage(fileReference, updatedUser)
        } else {
            // If there's an old image, delete it first
            deleteOldProfileImage(profileImageUrl) {
                uploadNewImage(fileReference, updatedUser)
            }
        }
    }

    // Method to upload the new image
    private fun uploadNewImage(fileReference: StorageReference, updatedUser: User) {
        selectedImageUri?.let { uri ->
            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updatedUser.profileImageUrl = downloadUrl.toString()
                        updateUser(updatedUser)
                    }.addOnFailureListener { e ->
                        handleImageUploadFailure(e)
                        progressBar.visibility = View.GONE // Hide progress bar on failure
                    }
                }.addOnFailureListener { e ->
                    handleImageUploadFailure(e)
                    progressBar.visibility = View.GONE // Hide progress bar on failure
                }
        }
    }

    private fun deleteOldProfileImage(oldPath: String?, onComplete: () -> Unit) {
        if (!oldPath.isNullOrEmpty()) { // Check if the oldPath is not null or empty
            FirebaseStorage.getInstance().getReferenceFromUrl(oldPath).delete()
                .addOnSuccessListener {
                    Log.d("ProfileUpdate", "Old profile image deleted successfully")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileUpdate", "Failed to delete old profile image", e)
                    onComplete() // Proceed even if deletion fails
                }
        } else {
            Log.d("ProfileUpdate", "No old profile image to delete")
            onComplete() // No old image to delete, proceed
        }
    }


    private fun updateUser(user: User) {
        userKey?.let { key ->
            val updates = mapOf(
                "firstName" to user.firstName,
                "middleName" to user.middleName,
                "lastName" to user.lastName,
                "name" to user.name,
                "email" to user.email,
                "profileImageUrl" to user.profileImageUrl,
                "address" to user.address
            )

            databaseReference.child(key).updateChildren(updates)
                .addOnCompleteListener { task ->
                    // Hide progress bar after the update is complete
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileUpdate", "Update failed", task.exception)
                    }
                }
        } ?: run {
            // Hide progress bar if user key is not found
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "User key not found, cannot update profile", Toast.LENGTH_SHORT).show()
        }
    }


    private fun handleImageUploadFailure(exception: Exception) {
        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
        Log.e("ProfileUpdate", "Image upload failed", exception)
    }

    private fun fetchUserKey() {
        email?.let { userEmail ->
            databaseReference.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        snapshot.children.forEach { dataSnapshot ->
                            userKey = dataSnapshot.key
                            firstName = dataSnapshot.child("firstName").value as? String
                            middleName = dataSnapshot.child("middleName").value as? String
                            lastName = dataSnapshot.child("lastName").value as? String
                            profileImageUrl = dataSnapshot.child("profileImageUrl").value as? String

                            updateUI()
                        }
                    } else {
                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileUpdate", "Database error: ${error.message}", error.toException())
                }
            })
        } ?: run {
            Toast.makeText(requireContext(), "Email not provided", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        view?.findViewById<EditText>(R.id.etFirstName)?.setText(firstName)
        view?.findViewById<EditText>(R.id.etMiddleName)?.setText(middleName)
        view?.findViewById<EditText>(R.id.etLastName)?.setText(lastName)

        val profileImageView = view?.findViewById<ShapeableImageView>(R.id.profilePicture)

        if (!profileImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile)  // Default placeholder
                .error(R.drawable.profile)        // Default error image
                .into(profileImageView)
        } else {
            // Set a default placeholder if profileImageUrl is empty
            profileImageView?.setImageResource(R.drawable.profile)
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(
            firstName: String?,
            middleName: String?,
            lastName: String?,
            email: String?,
            profileImageUrl: String?,
            address:String?
        ) = MyProfileFragmentClient().apply {
            arguments = Bundle().apply {
                putString("FIRST_NAME", firstName)
                putString("MIDDLE_NAME", middleName)
                putString("LAST_NAME", lastName)
                putString("EMAIL", email)
                putString("PROFILE_IMAGE_URL", profileImageUrl)
                putString("ADDRESS", address)
            }
        }
    }
}
