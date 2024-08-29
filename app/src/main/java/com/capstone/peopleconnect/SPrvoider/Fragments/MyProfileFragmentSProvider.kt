package com.capstone.peopleconnect.SProvider.Fragments

import android.app.Activity
import android.content.Intent
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
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.Classes.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class MyProfileFragmentSProvider : Fragment() {

    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null
    private var email: String? = null
    private var profileImageUrl: String? = null

    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    private var selectedImageUri: Uri? = null
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
        val etEmail: EditText = view.findViewById(R.id.etEmail)
        val btnSave: Button = view.findViewById(R.id.btnSave)

        etFirstName.setText(firstName)
        etMiddleName.setText(middleName)
        etLastName.setText(lastName)
        etEmail.setText(email)

        // Load the profile image using Picasso
        Picasso.get()
            .load(profileImageUrl)
            .placeholder(R.drawable.profile1)
            .error(R.drawable.profile1)
            .into(profilePicture)

        editIcon.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveProfile() }
        backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        setupRealTimeProfileListener()  // Call here to make sure real-time updates work

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun saveProfile() {
        val updatedFirstName = view?.findViewById<EditText>(R.id.etFirstName)?.text.toString()
        val updatedMiddleName = view?.findViewById<EditText>(R.id.etMiddleName)?.text.toString()
        val updatedLastName = view?.findViewById<EditText>(R.id.etLastName)?.text.toString()
        val updatedEmail = view?.findViewById<EditText>(R.id.etEmail)?.text.toString()

        if (updatedEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(
            firstName = updatedFirstName,
            middleName = updatedMiddleName,
            lastName = updatedLastName,
            name = "$updatedFirstName $updatedMiddleName $updatedLastName",
            email = updatedEmail,
            profileImageUrl = profileImageUrl ?: ""
        )

        if (selectedImageUri != null) {
            val fileReference = storageReference.child("$updatedEmail.jpg")
            handleImageUpload(fileReference, updatedUser)
        } else {
            updateUser(updatedUser, userKey)
        }
    }

    private fun handleImageUpload(fileReference: StorageReference, updatedUser: User) {
        deleteOldProfileImage(profileImageUrl) {
            selectedImageUri?.let { uri ->
                fileReference.putFile(uri)
                    .addOnSuccessListener {
                        fileReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                            updatedUser.profileImageUrl = downloadUrl.toString()
                            updateUser(updatedUser, userKey)
                        }.addOnFailureListener { e ->
                            handleImageUploadFailure(e)
                        }
                    }.addOnFailureListener { e ->
                        handleImageUploadFailure(e)
                    }
            }
        }
    }

    private fun deleteOldProfileImage(oldPath: String?, onComplete: () -> Unit) {
        oldPath?.let {
            FirebaseStorage.getInstance().getReferenceFromUrl(it).delete()
                .addOnSuccessListener {
                    Log.d("ProfileUpdate", "Old profile image deleted successfully")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileUpdate", "Failed to delete old profile image", e)
                    onComplete() // Proceed even if deletion fails
                }
        } ?: onComplete() // No old image to delete, proceed
    }

    private fun updateUser(user: User, userKey: String?) {
        userKey?.let { key ->
            val updates = mapOf(
                "firstName" to user.firstName,
                "middleName" to user.middleName,
                "lastName" to user.lastName,
                "name" to user.name,
                "email" to user.email,
                "profileImageUrl" to user.profileImageUrl
            )

            databaseReference.child(key).updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileUpdate", "Update failed", task.exception)
                    }
                }
        }
    }

    private fun handleImageUploadFailure(exception: Exception) {
        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
        Log.e("ProfileUpdate", "Image upload failed", exception)
    }

    private fun setupRealTimeProfileListener() {
        email?.let { userEmail ->
            databaseReference.orderByChild("email").equalTo(userEmail).addValueEventListener(object : ValueEventListener {
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
                    Toast.makeText(requireContext(), "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileUpdate", "Data retrieval failed", error.toException())
                }
            })
        }
    }

    private fun updateUI() {
        view?.let { view ->
            val firstNameEditText: EditText = view.findViewById(R.id.etFirstName)
            val middleNameEditText: EditText = view.findViewById(R.id.etMiddleName)
            val lastNameEditText: EditText = view.findViewById(R.id.etLastName)
            val emailEditText: EditText = view.findViewById(R.id.etEmail)
            val profilePictureImageView: ImageView = view.findViewById(R.id.profilePicture)

            firstNameEditText.setText(firstName)
            middleNameEditText.setText(middleName)
            lastNameEditText.setText(lastName)
            emailEditText.setText(email)

            // Load the profile image using Picasso
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile1)
                .error(R.drawable.profile1)
                .into(profilePictureImageView) // Explicitly using ImageView
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(firstName: String?, middleName: String?, lastName: String?, email: String?, profileImageUrl: String?) =
            MyProfileFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("FIRST_NAME", firstName)
                    putString("MIDDLE_NAME", middleName)
                    putString("LAST_NAME", lastName)
                    putString("EMAIL", email)
                    putString("PROFILE_IMAGE_URL", profileImageUrl)
                }
            }
    }
}

