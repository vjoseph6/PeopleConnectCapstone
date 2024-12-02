package com.capstone.peopleconnect.Client

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.AddPostFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class AddPostClientFragment : Fragment() {

    private var email: String? = null
    private lateinit var postDesc: String
    private lateinit var loadingDialog: AlertDialog
    private val selectedImages = mutableListOf<Uri>()
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private lateinit var categorySpinner: Spinner
    private var categoryName: String = ""
    private lateinit var btnSavePost: Button
    private val categories = mutableListOf<String>()
    private lateinit var dateEditText: EditText
    private lateinit var startTimeEditText: EditText
    private lateinit var descriptionPostEditText: EditText
    private lateinit var endTimeEditText: EditText
    private var startTimeCalendar: Calendar? = null
    private var endTimeCalendar: Calendar? = null
    private var bookSTartDate: String? = null
    private lateinit var timeIcon1: ImageView
    private lateinit var timeIcon2: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayExistingImages()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post_client, container, false)

        val addImageView: ImageView = view.findViewById(R.id.addImage)
        btnSavePost = view.findViewById(R.id.btnSavePost)
        descriptionPostEditText = view.findViewById(R.id.descriptionPostEditText)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        startTimeEditText = view.findViewById(R.id.startTimeEditText)
        endTimeEditText = view.findViewById(R.id.endTimeEditText)

        val btnBack: ImageButton = view.findViewById(R.id.btnBackSProviderSKills)
        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        // Add this: Create and set the adapter
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = spinnerAdapter

        val categoryToSelect = arguments?.getString("CATEGORY")
        bookSTartDate = arguments?.getString("START_DATE")
        fetchCategories(categoryToSelect)

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                categoryName = categories[position]
                Log.d("Category Selected", "Selected category: $categoryName")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                categoryName = ""
            }
        }

        arguments?.let {
            val category = it.getString("CATEGORY")
            fetchCategories(category)
            if (category != null && category.isNotBlank() && categories.contains(category)) {
                // Set the spinner to the correct category
                setSpinnerSelection(categorySpinner, category)
            } else {
                // If no valid category, default to "Select Category" or show all categories
                categorySpinner.setSelection(0)
            }
        }


        // Load placeholder image with Glide
        Glide.with(this)
            .load(R.drawable.c_upload) // Placeholder drawable
            .into(addImageView)

        // Hide addImage and Upload Image text when images are displayed
        addImageView.setOnClickListener {
            if (selectedImages.size < 3) {
                openImagePicker()
            } else {
                Toast.makeText(requireContext(), "Maximum of 3 images allowed", Toast.LENGTH_SHORT).show()
            }
        }


        arguments?.let {
            email = it.getString("EMAIL")
            val postId = it.getString("POST_ID")

            if (postId != null) {
                descriptionPostEditText.setText(it.getString("DESCRIPTION"))
                val category = it.getString("CATEGORY")
                val startDate = it.getString("START_DATE")
                // Set spinner selection
                fetchCategories(category)
                startTimeEditText.setText(it.getString("START_TIME"))
                endTimeEditText.setText(it.getString("END_TIME"))

                displayExistingImages()

                btnSavePost.text = "Edit"
            } else {
                btnSavePost.text = "Save"
            }

        }

        // Set up button click listener
        btnSavePost.setOnClickListener {
            if (btnSavePost.text == "Edit") {
                val postId = arguments?.getString("POST_ID") ?: return@setOnClickListener
                editPost(postId) // Call the editPost function
            }else {
                if (!validateInputs()) {
                    // Optionally show a message to the user about invalid inputs
                    return@setOnClickListener
                }
                savePost() // Call the savePost function for a new post
            }
        }


        dateEditText = view.findViewById(R.id.dateEditText)
        dateEditText.visibility = View.GONE
        startTimeEditText = view.findViewById(R.id.startTimeEditText)
        endTimeEditText = view.findViewById(R.id.endTimeEditText)
        displayExistingImages()

        // Set up click listeners for date and time fields
        dateEditText.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                val calendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateEditText.setText(dateFormat.format(calendar.time))
            }
        }

        startTimeEditText.setOnClickListener {
            showStartDateTimePicker { calendar ->
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                startTimeEditText.setText(timeFormat.format(calendar.time))
                startTimeCalendar = calendar
            }
        }


        endTimeEditText.setOnClickListener {
            if (startTimeEditText.text.isEmpty()) {
                Toast.makeText(context, "Please select start time first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showEndDateTimePicker { calendar ->
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                endTimeEditText.setText(timeFormat.format(calendar.time))
                endTimeCalendar = calendar
            }
        }

        return view
    }

    private fun setSpinnerSelection(spinner: Spinner, category: String) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == category) {
                spinner.setSelection(i)
                return
            }
        }
        // Default to the first item if the category is not found
        spinner.setSelection(0)
    }




    private fun displayExistingImages() {
        val rootView = view ?: return
        val existingImageUrls = arguments?.getStringArrayList("IMAGES") ?: emptyList()
        selectedImages.clear() // Clear any previously selected images
        selectedImages.addAll(existingImageUrls.map { Uri.parse(it) }) // Add existing images to the list
        displaySelectedImages() // Call the existing function to refresh the layout
    }

    private fun editPost(postId: String) {
        // Validate inputs
        if (!validateInputs()) return

        // Show loading dialog
        showLoadingDialog()

        // Get existing images
        val existingImageUrls = arguments?.getStringArrayList("IMAGES") ?: emptyList()

        // Compare and find modified images
        val imagesToUpdate = selectedImages.mapIndexed { index, uri ->
            if (index < existingImageUrls.size && uri.toString() == existingImageUrls[index]) {
                // Image not changed
                existingImageUrls[index]
            } else {
                // Image changed or new
                uri
            }
        }

        // Separate modified URIs from existing URLs
        val updatedImages = imagesToUpdate.filterIsInstance<Uri>()
        val unchangedImages = imagesToUpdate.filterIsInstance<String>()

        if (updatedImages.isNotEmpty()) {
            // If new or modified images exist, upload them first
            uploadChangedImagesAndUpdatePost(postId, updatedImages, unchangedImages)
        } else {
            // If no new or modified images, update directly with existing data
            updatePostDirectly(postId)
        }
    }

    private fun uploadChangedImagesAndUpdatePost(postId: String, updatedImages: List<Uri>, unchangedImages: List<String>) {
        val newImageUrls = mutableListOf<String>()
        val failedUploads = false

        updatedImages.forEachIndexed { index, uri ->
            val fileName = "post_images/${System.currentTimeMillis()}_${uri.lastPathSegment}"
            val fileReference = storageReference.child(fileName)

            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        newImageUrls.add(downloadUri.toString())

                        // Check if all uploads are done
                        if (newImageUrls.size == updatedImages.size) {
                            // Combine unchanged and newly uploaded URLs
                            val allImages = unchangedImages.toMutableList().apply { addAll(newImageUrls) }
                            updatePostWithNewImages(postId, allImages)
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                        dismissLoadingDialog()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismissLoadingDialog()
                }
        }
    }

    private fun updatePostWithNewImages(postId: String, newImageUrls: List<String>) {
        val post = createPostData(postId, newImageUrls)
        updatePostInDatabase(postId, post)
    }


    private fun updatePostDirectly(postId: String) {
        // Retrieve existing post images from arguments
        val existingImageUrls = arguments?.getStringArrayList("IMAGES") ?: emptyList()
        val post = createPostData(postId, existingImageUrls)
        updatePostInDatabase(postId, post)
    }

    private fun updatePostInDatabase(postId: String, post: Post) {
        FirebaseDatabase.getInstance().reference.child("posts").child(postId)
            .setValue(post)
            .addOnSuccessListener {
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Post updated successfully", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Failed to update post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createPostData(postId: String, imageUrls: List<String>): Post {
        return Post(
            postId = postId,
            postDescription = descriptionPostEditText.text.toString().trim(),
            email = email ?: "",
            categoryName = categorySpinner.selectedItem.toString(),
            postImages = imageUrls,
            postStatus = "Pending",
            client = true,
            startDate = bookSTartDate,
            bookDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            startTime = startTimeEditText.text.toString(),
            endTime = endTimeEditText.text.toString()
        )
    }



    fun showLoadingDialog() {
        // Create the dialog builder
        val dialogBuilder = AlertDialog.Builder(requireContext())

        // Inflate the custom layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.pre_loader, null) // Your custom layout for the loader

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
        var failedUploads = false

        selectedImages.forEach { uri ->
            val fileName = "post_images/${System.currentTimeMillis()}_${uri.lastPathSegment}"
            val fileReference = storageReference.child(fileName)
            uploadTasks.add(fileReference)

            fileReference.putFile(uri)
                .addOnSuccessListener {
                    if (failedUploads) return@addOnSuccessListener // Skip if any upload failed

                    fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())
                        if (imageUrls.size == selectedImages.size) {
                            savePostToDatabase(imageUrls)
                        }
                    }.addOnFailureListener { e ->
                        failedUploads = true
                        Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                        dismissLoadingDialog()
                    }
                }
                .addOnFailureListener { e ->
                    failedUploads = true
                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismissLoadingDialog()
                }
        }
    }

    private fun savePostToDatabase(imageUrls: List<String>) {
        try {
            val postId = FirebaseDatabase.getInstance().reference.child("posts").push().key ?: return
            val postDescEditText: EditText = view?.findViewById(R.id.descriptionPostEditText) ?: return
            postDesc = postDescEditText.text.toString().trim()

            when {
                categoryName.isEmpty() -> {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                    return
                }

                postDesc.isEmpty() -> {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show()
                    return
                }

                startTimeEditText.text.isEmpty() -> {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "Please select a start time", Toast.LENGTH_SHORT).show()
                    return
                }
                endTimeEditText.text.isEmpty() -> {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "Please select an end time", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val post = Post(
                postId = postId,
                postDescription = postDesc,
                email = email,
                categoryName = categoryName,
                postImages = imageUrls,
                postStatus = "Pending",
                client = true,
                startDate = bookSTartDate.toString() ?: "",
                bookDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
                startTime = startTimeEditText.text.toString(),
                endTime = endTimeEditText.text.toString()
            )

            // Save to Firebase
            FirebaseDatabase.getInstance().reference.child("posts").child(postId)
                .setValue(post)
                .addOnSuccessListener {
                    try {
                        if (isAdded && context != null) {  // Ensure fragment is still attached
                            dismissLoadingDialog()
                            Toast.makeText(requireContext(), "Post saved successfully", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Ensure you catch any issues here
                        dismissLoadingDialog() // Make sure dialog is closed in case of errors
                        Toast.makeText(requireContext(), "Error occurred during cleanup: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    try {
                        if (isAdded && context != null) {  // Ensure fragment is still attached
                            dismissLoadingDialog()
                            Toast.makeText(requireContext(), "Failed to save post: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Ensure you catch any issues here
                        dismissLoadingDialog() // Make sure dialog is closed in case of errors
                    }
                }
        } catch (e: Exception) {
            dismissLoadingDialog()
            Toast.makeText(requireContext(), "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun validateInputs(): Boolean {

        if (descriptionPostEditText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show()
            return false
        }
        if (categorySpinner.selectedItem == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }
        if (startTimeEditText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a start time", Toast.LENGTH_SHORT).show()
            return false
        }
        if (endTimeEditText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select an end time", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select Pictures"),
            CLIENT_REQUEST_IMAGE_PICK
        )
    }

    private fun selectImageForReplacement() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, CLIENT_REQUEST_IMAGE_REPLACE)
    }

    // Function to add image to layout
    private fun displaySelectedImages() {
        val rootView = view ?: return
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
                    clientImageReplaceIndex = index
                    selectImageForReplacement()
                    true
                }
            }
            Glide.with(this).load(uri).into(imageView)
            imageContainer.addView(imageView)
        }
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
                CLIENT_REQUEST_IMAGE_PICK -> {
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
                CLIENT_REQUEST_IMAGE_REPLACE -> {
                    data.data?.let { uri ->
                        clientImageReplaceIndex?.let { index ->
                            selectedImages[index] = uri // Replace image at specific index
                            displaySelectedImages() // Refresh layout
                        }
                    }
                }
            }
        }
    }

    private fun fetchCategories(categoryToSelect: String? = null) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("category")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                categories.add("Select Category") // Default placeholder

                snapshot.children.forEach { categorySnapshot ->
                    categorySnapshot.child("Sub Categories").children.forEach { subCategorySnapshot ->
                        subCategorySnapshot.child("name").getValue(String::class.java)?.let {
                            categories.add(it)
                        }
                    }
                }

                // Notify adapter of data change
                (categorySpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()

                // Set spinner selection to match `categoryToSelect`
                categoryToSelect?.let { setSpinnerSelection(categorySpinner, it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }





    private fun showDatePicker(onDateSet: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onDateSet(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showStartDateTimePicker(onDateTimeSet: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val initialMinute = Calendar.getInstance().get(Calendar.MINUTE)

                showTimePicker(year, month, dayOfMonth, initialHour, initialMinute) { hour, minute ->
                    startTimeCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hour, minute)
                    }

                    // Format and set the bookDay based on startTime
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    dateEditText.setText(dateFormat.format(startTimeCalendar!!.time))
                    bookSTartDate = dateFormat.format(startTimeCalendar!!.time)

                    onDateTimeSet(startTimeCalendar!!)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }


    private fun showEndDateTimePicker(onDateTimeSet: (Calendar) -> Unit) {
        if (startTimeCalendar == null) {
            Toast.makeText(requireContext(), "Please select start time first", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the same date as start time
        val year = startTimeCalendar!!.get(Calendar.YEAR)
        val month = startTimeCalendar!!.get(Calendar.MONTH)
        val dayOfMonth = startTimeCalendar!!.get(Calendar.DAY_OF_MONTH)

        val initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val initialMinute = Calendar.getInstance().get(Calendar.MINUTE)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                showTimePicker(year, month, dayOfMonth, initialHour, initialMinute) { hour, minute ->
                    val endTimeCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hour, minute)
                    }

                    // Ensure end time is after start time
                    if (!endTimeCalendar.after(startTimeCalendar)) {
                        Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
                        return@showTimePicker
                    }

                    // Allow a time span that crosses midnight, but ensure the duration does not exceed 24 hours
                    val durationInMillis = endTimeCalendar.timeInMillis - startTimeCalendar!!.timeInMillis
                    val durationInHours = durationInMillis / (1000 * 60 * 60)

                    if (durationInHours > 24) {
                        Toast.makeText(requireContext(), "Booking duration cannot exceed 24 hours", Toast.LENGTH_SHORT).show()
                        return@showTimePicker
                    }

                    if (endTimeCalendar.get(Calendar.YEAR) == startTimeCalendar!!.get(Calendar.YEAR) &&
                        endTimeCalendar.get(Calendar.MONTH) == startTimeCalendar!!.get(Calendar.MONTH) &&
                        endTimeCalendar.get(Calendar.DAY_OF_MONTH) == startTimeCalendar!!.get(Calendar.DAY_OF_MONTH) &&
                        endTimeCalendar.get(Calendar.HOUR_OF_DAY) == startTimeCalendar!!.get(Calendar.HOUR_OF_DAY) &&
                        endTimeCalendar.get(Calendar.MINUTE) == startTimeCalendar!!.get(Calendar.MINUTE)) {
                        Toast.makeText(requireContext(), "Start and end time cannot be the same", Toast.LENGTH_SHORT).show()
                        return@showTimePicker
                    }

                    // Format and set the end time
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    dateEditText.setText(dateFormat.format(startTimeCalendar!!.time))  // You may want to show the end date here

                    onDateTimeSet(endTimeCalendar)
                }
            },
            startTimeCalendar!!.get(Calendar.YEAR),
            startTimeCalendar!!.get(Calendar.MONTH),
            startTimeCalendar!!.get(Calendar.DAY_OF_MONTH)
        ).apply {
            show()
        }
    }


    private fun showTimePicker(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        initialHour: Int,
        initialMinute: Int,
        onTimeSet: (Int, Int) -> Unit
    ) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hourOfDay, minute)
                }

                // Only validate against current time if it's today
                val currentTime = Calendar.getInstance()
                if (year == currentTime.get(Calendar.YEAR) &&
                    month == currentTime.get(Calendar.MONTH) &&
                    dayOfMonth == currentTime.get(Calendar.DAY_OF_MONTH) &&
                    selectedTime.before(currentTime)
                ) {
                    Toast.makeText(requireContext(), "Selected time cannot be in the past.", Toast.LENGTH_SHORT).show()
                } else {
                    onTimeSet(hourOfDay, minute)
                }
            },
            initialHour,
            initialMinute,
            false
        )

        timePickerDialog.show()
    }

    companion object {
        @JvmStatic
        fun newInstance(email: String, post: Post? = null) =
            AddPostClientFragment().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email) // Always pass the email
                    if (post != null) {
                        putString("DESCRIPTION", post.postDescription)
                        putString("CATEGORY", post.categoryName)
                        putString("START_TIME", post.startTime)
                        putString("END_TIME", post.endTime)
                        putString("START_DATE", post.startDate)
                        putStringArrayList("IMAGES", ArrayList(post.postImages))
                        putBoolean("IS_FROM_CLIENT_ADAPTER", true) // Tag to indicate editing
                        putString("POST_ID", post.postId) // Pass the post ID for editing
                    }
                }
            }

        // Using different values from AddPostFragment
        private const val CLIENT_REQUEST_IMAGE_PICK = 3001
        private const val CLIENT_REQUEST_IMAGE_REPLACE = 3002
        private var clientImageReplaceIndex: Int? = null
    }

    // Add this to ensure dialog is dismissed when fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        dismissLoadingDialog()
    }
}