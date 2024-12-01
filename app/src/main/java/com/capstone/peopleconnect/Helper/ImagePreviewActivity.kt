package com.capstone.peopleconnect.Helper

import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var imageUrl: String
    private lateinit var email: String
    private lateinit var loadingDialog: android.app.AlertDialog
    private lateinit var categoryName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        // Retrieve data passed from the previous activity
        imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        email = intent.getStringExtra("EMAIL") ?: ""
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        // Load the image into the ImageView
        val imageView: ImageView = findViewById(R.id.fullScreenImage)
        Picasso.get().load(imageUrl).into(imageView)

        // Set up the delete button to show a confirmation dialog
        val btnDelete: ImageButton = findViewById(R.id.deleteButton)
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.sprovider_dialog_logout, null)
        val tvTitle: TextView = dialogView.findViewById(R.id.tvLogoutTitle)
        val btnDelete: Button = dialogView.findViewById(R.id.btnLogout)
        val tvCancel: TextView = dialogView.findViewById(R.id.tvCancel)

        // Customize dialog text for delete confirmation
        tvTitle.text = "Do you want to delete this image?"
        btnDelete.text = "Delete"

        // Create and display the AlertDialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        alertDialog.show()

        // Set up Delete button action
        btnDelete.setOnClickListener {
            deleteImage()
            alertDialog.dismiss()
        }

        // Set up Cancel button action
        tvCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deleteImage() {
        // Delete the image from Firebase Storage
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageReference.delete()
            .addOnSuccessListener {
                // After deleting from storage, remove the URL from Firebase Database
                val databaseReference = FirebaseDatabase.getInstance().getReference("posts")
                databaseReference.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (postSnapshot in snapshot.children) {
                                val postCategory = postSnapshot.child("categoryName").getValue(String::class.java)
                                if (postCategory == categoryName) {
                                    val imagesSnapshot = postSnapshot.child("postImages")
                                    for (imageSnapshot in imagesSnapshot.children) {
                                        if (imageSnapshot.getValue(String::class.java) == imageUrl) {
                                            imageSnapshot.ref.removeValue()
                                            finish() // Close the activity after deletion
                                            break
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error if necessary
                        }
                    })
            }
            .addOnFailureListener {
                // Handle storage deletion failure if necessary
            }
    }

    fun showLoadingDialog() {
        // Create the dialog builder
        val dialogBuilder = android.app.AlertDialog.Builder(this)

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.pre_loader, null) // Your custom layout for the loader

        // Find the ProgressBar in the inflated view
        val progressBar: ProgressBar = dialogView.findViewById(R.id.progressBar_pre_loader)

        // Change the color directly here
        progressBar.indeterminateDrawable.setColorFilter(
            ContextCompat.getColor(this, R.color.blue),
            PorterDuff.Mode.SRC_IN // Set the mode for coloring
        )

        // Set the custom layout to the dialog
        dialogBuilder.setView(dialogView)

        // Create and show the dialog
        loadingDialog = dialogBuilder.create()
        loadingDialog.setCancelable(false) // Prevent dismissing on back press

        loadingDialog.show()
    }



    fun dismissLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

}