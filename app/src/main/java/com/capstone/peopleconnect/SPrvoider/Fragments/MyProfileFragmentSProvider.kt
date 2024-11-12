package com.capstone.peopleconnect.SProvider.Fragments

import android.Manifest
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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Client.Fragments.MyProfileFragmentClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.util.Locale

class MyProfileFragmentSProvider : Fragment() {

    private lateinit var locationBtn: ImageView
    private lateinit var progressBar: ProgressBar
    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null
    private var email: String? = null
    private var profileImageUrl: String? = null
    private var userAddress: String? = null
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var addressEditText: EditText
    private var selectedImageUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var userKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            firstName = it.getString("FIRST_NAME")
            middleName = it.getString("MIDDLE_NAME")
            lastName = it.getString("LAST_NAME")
            email = it.getString("EMAIL")
            profileImageUrl = it.getString("PROFILE_IMAGE_URL")
            userAddress = it.getString("ADDRESS")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_profile_s_provider, container, false)

        val backButton: ImageView = view.findViewById(R.id.btnBack)
        val profilePicture: ImageView = view.findViewById(R.id.profilePicture)
        val editIcon: ImageView = view.findViewById(R.id.editIcon)
        val etFirstName: EditText = view.findViewById(R.id.etFirstName)
        val etMiddleName: EditText = view.findViewById(R.id.etMiddleName)
        val etLastName: EditText = view.findViewById(R.id.etLastName)
        val etAddress: EditText = view.findViewById(R.id.address)
        val btnSave: Button = view.findViewById(R.id.btnSave)

        etFirstName.setText(firstName)
        etMiddleName.setText(middleName)
        etLastName.setText(lastName)
        etAddress.setText(userAddress)

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
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile)  // Default placeholder
                .error(R.drawable.profile)        // Default error image
                .into(profilePicture)
        } else {
            // Set a default placeholder if profileImageUrl is empty
            profilePicture.setImageResource(R.drawable.profile)
        }

        editIcon.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveProfile() }
        backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        setupRealTimeProfileListener()  // Call here to make sure real-time updates work

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
                if (addresses!= null && addresses.isNotEmpty()) {
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
        progressBar.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "firstName" to (view?.findViewById<EditText>(R.id.etFirstName)?.text.toString() ?: ""),
            "middleName" to (view?.findViewById<EditText>(R.id.etMiddleName)?.text.toString() ?: ""),
            "lastName" to (view?.findViewById<EditText>(R.id.etLastName)?.text.toString() ?: ""),
            "address" to (view?.findViewById<EditText>(R.id.address)?.text.toString() ?: "")
        )

        // Add full name to updates
        updates["name"] = "${updates["firstName"]} ${updates["middleName"]} ${updates["lastName"]}"

        if (selectedImageUri != null) {
            uploadImage(updates)
        } else {
            updateUserData(updates)
        }
    }

    private fun uploadImage(updates: HashMap<String, Any>) {
        val fileReference = storageReference.child("$email.jpg")

        // Delete old image if exists
        if (!profileImageUrl.isNullOrEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl!!).delete()
                .addOnFailureListener { e ->
                    Log.e("ProfileUpdate", "Failed to delete old image", e)
                }
        }

        // Upload new image
        selectedImageUri?.let { uri ->
            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            updates["profileImageUrl"] = downloadUrl.toString()
                            updateUserData(updates)
                        }
                        .addOnFailureListener { e ->
                            handleError("Failed to get download URL", e)
                        }
                }
                .addOnFailureListener { e ->
                    handleError("Failed to upload image", e)
                }
        }
    }

    private fun updateUserData(updates: HashMap<String, Any>) {
        userKey?.let { key ->
            databaseReference.child(key).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    handleError("Failed to update profile", e)
                }
                .addOnCompleteListener {
                    progressBar.visibility = View.GONE
                }
        } ?: handleError("User key not found", null)
    }

    private fun handleError(message: String, exception: Exception?) {
        progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        exception?.let {
            Log.e("ProfileUpdate", message, it)
        }
    }

    private fun setupRealTimeProfileListener() {
        email?.let { userEmail ->
            databaseReference.orderByChild("email").equalTo(userEmail)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            snapshot.children.firstOrNull()?.let { dataSnapshot ->
                                userKey = dataSnapshot.key
                                firstName = dataSnapshot.child("firstName").value as? String
                                middleName = dataSnapshot.child("middleName").value as? String
                                lastName = dataSnapshot.child("lastName").value as? String
                                profileImageUrl = dataSnapshot.child("profileImageUrl").value as? String
                                updateUI()
                            }
                        } else {
                            handleError("User not found", null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        handleError("Failed to retrieve user data", error.toException())
                    }
                })
        }
    }

    private fun updateUI() {
        view?.let { view ->
            view.findViewById<EditText>(R.id.etFirstName)?.setText(firstName)
            view.findViewById<EditText>(R.id.etMiddleName)?.setText(middleName)
            view.findViewById<EditText>(R.id.etLastName)?.setText(lastName)

            val profilePictureImageView = view.findViewById<ImageView>(R.id.profilePicture)
            if (!profileImageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile1)
                    .error(R.drawable.profile1)
                    .into(profilePictureImageView)
            } else {
                profilePictureImageView.setImageResource(R.drawable.profile1)
            }
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
        ) = MyProfileFragmentSProvider().apply {
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

