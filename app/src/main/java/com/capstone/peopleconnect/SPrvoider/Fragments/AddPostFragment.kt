package com.capstone.peopleconnect.SPrvoider.Fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPostFragment : Fragment() {
    private var email: String? = null
    private var categoryName: String? = null
    private lateinit var postDesc: String
    private lateinit var loadingDialog: AlertDialog
    private val selectedImages = mutableListOf<Uri>()
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
            categoryName = it.getString("CATEGORY_NAME")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        // Set category name
        val filterByTitle: TextView = view.findViewById(R.id.filter_by_title)
        filterByTitle.text = categoryName

        val addImageView: ImageView = view.findViewById(R.id.addImage)
        val btnSavePost: Button = view.findViewById(R.id.btnSavePost)
        val postDescEditText: EditText = view.findViewById(R.id.descriptionPostEditText)
        postDesc = postDescEditText.text.toString()

        val btnBack: ImageButton = view.findViewById(R.id.btnBackSProviderSKills)
        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        // Load placeholder image with Glide
        Glide.with(this)
            .load(R.drawable.s_upload) // Placeholder drawable
            .into(addImageView)

        // Hide addImage and Upload Image text when images are displayed
        addImageView.setOnClickListener {
            if (selectedImages.size < 3) {
                openImagePicker()
            } else {
                Toast.makeText(requireContext(), "Maximum of 3 images allowed", Toast.LENGTH_SHORT).show()
            }
        }

        btnSavePost.setOnClickListener {
            savePost()
        }

        return view
    }

    fun showLoadingDialog() {
        // Create the dialog builder
        val dialogBuilder = AlertDialog.Builder(requireContext())

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.pre_loader, null) // Your custom layout for the loader

        // Find the ProgressBar in the inflated view
        val progressBar: ProgressBar = dialogView.findViewById(R.id.progressBar_pre_loader)

        // Change the color directly here
        progressBar.indeterminateDrawable.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.blue),
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

    private fun savePost() {
        showLoadingDialog()
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), "Please select images to upload", Toast.LENGTH_SHORT).show()
            dismissLoadingDialog()
            return
        }

        val imageUrls = mutableListOf<String>()
        val uploadTasks = mutableListOf<StorageReference>()

        selectedImages.forEach { uri ->
            val fileName = "post_images/${System.currentTimeMillis()}_${uri.lastPathSegment}"
            val fileReference = storageReference.child(fileName)
            uploadTasks.add(fileReference)

            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                        if (imageUrls.size == selectedImages.size) {
                            savePostToDatabase(imageUrls)
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                        dismissLoadingDialog()
                    }
                }
        }
    }

    private fun savePostToDatabase(imageUrls: List<String>) {
        val postId = FirebaseDatabase.getInstance().reference.child("posts").push().key ?: return
        val postDescEditText: EditText = view?.findViewById(R.id.descriptionPostEditText) ?: return
        postDesc = postDescEditText.text.toString().trim()

        if (postDesc.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT)
                .show()
            return // Exit if the description is empty
        }

        // Create an instance of the Post data class
        val post = Post(
            postId = postId,
            postDescription = postDesc,
            email = email,
            bookDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            categoryName = categoryName.toString(),
            postImages = imageUrls,
            postStatus = "Pending"
        )

        // Save the post instance to Firebase
        FirebaseDatabase.getInstance().reference.child("posts").child(postId).setValue(post)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Post saved successfully", Toast.LENGTH_SHORT)
                        .show()
                    dismissLoadingDialog()

                    // Finish the fragment after success
                    requireActivity().supportFragmentManager.popBackStack()  // This pops the fragment off the back stack
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save post: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismissLoadingDialog()
                }
            }
        }

        private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Allow multiple selection
        }
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), REQUEST_IMAGE_PICK)
    }


    // Function to add image to layout
    private fun displaySelectedImages() {
        val imageContainer: LinearLayout = requireView().findViewById(R.id.imageContainer)
        val addImage: ImageView = requireView().findViewById(R.id.addImage)
        val uploadImageText: TextView = requireView().findViewById(R.id.uploadTxt)

        // Clear the image container before adding new images
        imageContainer.removeAllViews()

        // Check if the maximum number of images is reached
        if (selectedImages.size >= 3) {
            addImage.visibility = View.GONE
            uploadImageText.visibility = View.GONE
        } else {
            addImage.visibility = View.VISIBLE
            uploadImageText.visibility = View.VISIBLE
        }

        selectedImages.forEachIndexed { index, uri ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                    setMargins(10, 10, 10, 10)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setOnClickListener { showFullscreenImage(uri) }
                setOnLongClickListener {
                    imageReplaceIndex = index
                    selectImageForReplacement()
                    true
                }
            }
            Glide.with(this).load(uri).into(imageView)
            imageContainer.addView(imageView)
        }
    }


    // Function to handle image selection for replacement
    private fun selectImageForReplacement() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_IMAGE_REPLACE) // Start replacement intent
    }

    // Fullscreen image display function
    private fun showFullscreenImage(uri: Uri) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val fullscreenImage: ImageView = dialog.findViewById(R.id.fullscreenImageView)
        Glide.with(this).load(uri).into(fullscreenImage)

        dialog.show()
    }

    // Handling onActivityResult to replace image and handle multiple selections
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    // Check if multiple images were selected
                    data.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            if (selectedImages.size < 3) { // Enforce a limit of 3 images
                                selectedImages.add(imageUri)
                            } else {
                                Toast.makeText(requireContext(), "Maximum of 3 images allowed", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }
                    } ?: run {
                        // If only one image was selected
                        data.data?.let { imageUri ->
                            if (selectedImages.size < 3) {
                                selectedImages.add(imageUri)
                            } else {
                                Toast.makeText(requireContext(), "Maximum of 3 images allowed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    displaySelectedImages() // Refresh layout with all selected images
                }
                REQUEST_IMAGE_REPLACE -> {
                    data.data?.let { uri ->
                        imageReplaceIndex?.let { index ->
                            selectedImages[index] = uri // Replace image at specific index
                            displaySelectedImages() // Refresh layout
                        }
                    }
                }
            }
        }
    }

    companion object {

        private const val REQUEST_IMAGE_PICK = 1002
        private const val REQUEST_IMAGE_REPLACE = 2
        private var imageReplaceIndex: Int? = null
        fun newInstance(email: String, categoryName: String) =
            AddPostFragment().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("CATEGORY_NAME", categoryName)
                }
            }
    }
}