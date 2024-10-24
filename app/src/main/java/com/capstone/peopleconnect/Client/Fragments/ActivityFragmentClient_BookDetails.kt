package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Helper.StripeHelper
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class ActivityFragmentClient_BookDetails : Fragment() {

    private var serviceOffered: String? = null
    private var userEmail: String? = null
    private lateinit var dateEditText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var dateIcon: ImageView
    private lateinit var startIcon: ImageView
    private lateinit var endIcon: ImageView
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var bookNow: Button
    private lateinit var locationEditText: EditText
    private lateinit var databaseReference: DatabaseReference
    private lateinit var rate: String
    private var startTimeCalendar: Calendar? = null
    private var endTimeCalendar: Calendar? = null
    private lateinit var descEditText: EditText
    private val imageUris = mutableListOf<Uri?>() // Store selected image URIs
    private lateinit var displayImagesLayout: LinearLayout
    private lateinit var firstImage: ShapeableImageView
    private lateinit var secondImage: ShapeableImageView
    private lateinit var thirdImage: ShapeableImageView
    private lateinit var btnAddImages: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImagePosition = -1
    private lateinit var loadingDialog: AlertDialog
    private lateinit var stripeHelper: StripeHelper
    private var bookDay: String? = null
    private var startTime: String? = null
    private var endTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_client__book_details, container, false)

        arguments.let {
            bookDay = arguments?.getString("bookDay") ?: ""
            startTime = arguments?.getString("startTime") ?: ""
            endTime = arguments?.getString("endTime") ?: ""
        }

        stripeHelper = StripeHelper(requireContext(), this) // 'this' refers to the fragment's lifecycle owner


        // Initialize views
        dateIcon = view.findViewById(R.id.dateIcon)
        startIcon = view.findViewById(R.id.timeIcon1)
        endIcon = view.findViewById(R.id.timeIcon2)
        dateEditText = view.findViewById(R.id.dateEditText)
        descEditText = view.findViewById(R.id.etDescription)
        descEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                descEditText.clearFocus() // Remove focus from the EditText
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(descEditText.windowToken, 0) // Hide the keyboard
                true // Consume the action
            } else {
                false // Do not consume the action
            }
        }
        startTimeEditText = view.findViewById(R.id.startTime)
        endTimeEditText = view.findViewById(R.id.endTime)
        btnAddImages = view.findViewById(R.id.addImage)
        Glide.with(this)
            .load(R.drawable.upload)  // Replace with your drawable resource or image URL
            .into(btnAddImages)
        locationEditText = view.findViewById(R.id.etSelectLocation)

        dateEditText.setText(bookDay?.ifEmpty { "" } ?: "")
        // Set the start time and calculate the hour rate
        startTimeEditText.setText(startTime?.ifEmpty { "" } ?: "").also {  calculateHourRate() }

        endTimeEditText.setText(endTime?.ifEmpty { "" } ?: "").also { calculateHourRate() }


        //save booking
        bookNow = view.findViewById(R.id.btnBookNow)
        /*bookNow.setOnClickListener { saveBooking() }*/
        bookNow.setOnClickListener { initiatePayment() }



        //for the upload image
        displayImagesLayout = view.findViewById(R.id.displayImages)
        firstImage = view.findViewById(R.id.firstImage)
        secondImage = view.findViewById(R.id.secondImage)
        thirdImage = view.findViewById(R.id.thirdImage)

        setupImagePicker()
        setupLongClickReplace()


        // Get address by email
        val email = arguments?.getString("EMAIL")
        email?.let { getAddressByEmail(it) }

        val btnBackClient: ImageButton = view.findViewById(R.id.btnBackClient)
        btnBackClient.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }


        // Set rate
        rate = arguments?.getString("SKILL_RATE") ?: "0"
        val rateTextView = view.findViewById<TextView>(R.id.etRate)
        rateTextView.text = rate

        // Set default hour rate
        val hourRateEditText = view.findViewById<EditText>(R.id.etHourRate)
        hourRateEditText.setText("1")
        hourRateEditText.addTextChangedListener(createRateTextWatcher(rateTextView))



        // Date and time pickers
        dateIcon.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                val selectedDate = "$year-${month + 1}-$dayOfMonth"
                dateEditText.setText(selectedDate)
            }
        }


        startIcon.setOnClickListener {
            showStartDateTimePicker { startTime ->
                startTimeEditText.setText(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(startTime.time))
                startTimeCalendar = startTime // Store the full Calendar object
                calculateHourRate() // Pass both start and end Calendar objects
            }
        }

        endIcon.setOnClickListener {
            showEndDateTimePicker { endTime ->
                endTimeEditText.setText(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(endTime.time))
                endTimeCalendar = endTime // Store the full Calendar object
                calculateHourRate() // Pass both start and end Calendar objects
            }
        }

        return view
    }

    private fun initiatePayment() { //this communicates to stripe helper that will make it generate a payment sheet
        showLoadingDialog()
        val userEmail = auth.currentUser?.email ?: ""
        val providerEmail = arguments?.getString("EMAIL") ?: ""
        val serviceOffered = arguments?.getString("SERVICE_OFFERED") ?: ""
        val bookingAmountString = view?.findViewById<EditText>(R.id.etRate)?.text?.toString()
        val bookingAmount = bookingAmountString?.toDoubleOrNull() ?: 0.0
        val currency = "php"


        // Validate input fields
        if (providerEmail.isEmpty() ||
            serviceOffered.isEmpty() ||
            startTimeEditText.text.isEmpty() ||
            endTimeEditText.text.isEmpty() ||
            descEditText.text.isEmpty() ||
            dateEditText.text.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all the required fields.", Toast.LENGTH_SHORT).show()
            dismissLoadingDialog()
            return
        }

        // Initiate payment
        stripeHelper.fetchPayment(bookingAmount, currency, userEmail, providerEmail, serviceOffered)
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


    // checkForDuplicateBooking now also checks for pending status
    private fun checkForDuplicateBooking(
        userEmail: String,
        providerEmail: String,
        bookingDay: String, // Add bookingDay here
        bookingStartTime: String,
        bookingEndTime: String,
        serviceOffered: String,
        onResult: (Boolean) -> Unit
    ) {
        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")

        // Convert booking times into minutes since midnight for easier comparison
        val currentBookingStartMinutes = convertToMinutesSinceMidnight(bookingStartTime)
        val currentBookingEndMinutes = convertToMinutesSinceMidnight(bookingEndTime)

        bookingReference.orderByChild("providerEmail").equalTo(providerEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var duplicateFound = false

                    for (bookingSnapshot in dataSnapshot.children) {
                        val existingBooking = bookingSnapshot.getValue(Bookings::class.java)

                        if (existingBooking != null &&
                            existingBooking.serviceOffered == serviceOffered &&
                            existingBooking.bookingStatus == "Pending"
                        ) {
                            // Convert the existing booking times to minutes since midnight for comparison
                            val existingStartMinutes = convertToMinutesSinceMidnight(existingBooking.bookingStartTime)
                            val existingEndMinutes = convertToMinutesSinceMidnight(existingBooking.bookingEndTime)

                            // Check for time overlap
                            if (isTimeOverlap(
                                    currentBookingStartMinutes,
                                    currentBookingEndMinutes,
                                    existingStartMinutes,
                                    existingEndMinutes
                                )
                            ) {
                                duplicateFound = true
                                break
                            }
                        }
                    }
                    onResult(duplicateFound)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(requireContext(), "Error checking for duplicate: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            })
    }

    private fun isTimeOverlap(
        currentStart: Int,
        currentEnd: Int,
        existingStart: Int,
        existingEnd: Int
    ): Boolean {
        // Handle cross-day bookings (e.g., 11:45 PM to 12:00 AM)
        val currentEndAdjusted = if (currentStart > currentEnd) currentEnd + 1440 else currentEnd // Add 1440 min (24 hours) if crossing day
        val existingEndAdjusted = if (existingStart > existingEnd) existingEnd + 1440 else existingEnd

        // Check for overlap
        return currentStart < existingEndAdjusted && currentEndAdjusted > existingStart
    }

    private fun convertToMinutesSinceMidnight(time: String): Int {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault()) // e.g., 11:45 PM or 12:00 AM
        val date = format.parse(time)

        val calendar = Calendar.getInstance()
        calendar.time = date

        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)

        return hours * 60 + minutes
    }


    fun saveBooking() {
        dismissLoadingDialog()
        showLoadingDialog() // Show loading dialog immediately

        userEmail = auth.currentUser?.email ?: ""
        val providerEmail = arguments?.getString("EMAIL") ?: ""
        val bookingStatus = "Pending"
        serviceOffered = arguments?.getString("SERVICE_OFFERED") ?: ""
        val bookingStartTime = startTimeEditText.text.toString()
        val bookingEndTime = endTimeEditText.text.toString()
        val bookingDescription = descEditText.text.toString()
        val bookingDay = dateEditText.text.toString()
        val bookingLocation = locationEditText.text.toString()
        val bookingAmountString = view?.findViewById<EditText>(R.id.etRate)?.text?.toString()
        val bookingAmount = bookingAmountString?.toDoubleOrNull() ?: 0.0 // Fallback to 0.0 if the conversion fails
        Log.d("save booking", "$bookingAmount")
        val bookingPaymentMethod = ""

        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")
        val bookingId = bookingReference.push().key ?: return

        val validImageUris = imageUris.filterNotNull()

        // Check for duplicate booking first
        checkForDuplicateBooking(userEmail.toString(), providerEmail, bookingDay, bookingStartTime, bookingEndTime, serviceOffered.toString()) { duplicateFound ->
            if (duplicateFound) {
                requireContext().let {
                    dismissLoadingDialog()
                    Toast.makeText(it, "You already have a pending booking with this provider for this service.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Proceed to upload images
                Log.d("SaveBooking", "Starting image upload...")
                uploadImagesToFirebase(userEmail.toString(), validImageUris, bookingId) { imageUrls ->
                    if (imageUrls.isNotEmpty()) {
                        val booking = Bookings(
                            bookByEmail = userEmail.toString(),
                            providerEmail = providerEmail,
                            bookingStatus = bookingStatus,
                            serviceOffered = serviceOffered.toString(),
                            bookingStartTime = bookingStartTime,
                            bookingEndTime = bookingEndTime,
                            bookingDescription = bookingDescription,
                            bookingDay = bookingDay,
                            bookingLocation = bookingLocation,
                            bookingAmount = bookingAmount,
                            bookingPaymentMethod = bookingPaymentMethod,
                            bookingCancelClient = "",
                            bookingCancelProvider = "",
                            bookingUploadImages = imageUrls
                        )

                        // Save booking to the database
                        Log.d("SaveBooking", "Saving booking to database...")
                        saveBookingToDatabase(bookingId, booking)
                    } else {
                        Log.e("SaveBooking", "Image upload failed.")
                        Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                    dismissLoadingDialog() // Dismiss loading dialog after all operations are done
                }
            }
        }
    }



    // Upload images to Firebase Storage
    private fun uploadImagesToFirebase(userEmail: String, uris: List<Uri>, bookingId: String, onComplete: (List<String>) -> Unit) {
        if (uris.isEmpty()) {
            onComplete(emptyList()) // No images to upload
            return
        }

        val storageReference = FirebaseStorage.getInstance().reference
        val imageUrls = mutableListOf<String>()

        uris.forEachIndexed { index, uri ->
            val fileReference = storageReference.child("bookingImages/$userEmail/${UUID.randomUUID()}")

            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                        imageUrls.add(downloadUrl.toString())

                        // Check if all images are uploaded
                        if (imageUrls.size == uris.size) {
                            onComplete(imageUrls)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to upload image $index", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Save booking to Realtime Database
    private fun saveBookingToDatabase(bookingId: String, booking: Bookings) {
        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")

        bookingReference.child(bookingId).setValue(booking)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    dismissLoadingDialog()
                    Toast.makeText(requireContext(), "Booking saved successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to ActivityFragmentClient after successful booking
                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.frame_layout, ActivityFragmentClient.newInstance(email = userEmail.toString()))
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                } else {
                    Toast.makeText(requireContext(), "Failed to save booking", Toast.LENGTH_SHORT).show()
                }
            }
    }




    // Image picker intent
    private fun setupImagePicker() {
        btnAddImages.setOnClickListener {
            descEditText.clearFocus() // Remove focus from the EditText
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(descEditText.windowToken, 0) // Hide the keyboard
            true // Consume the action

            if (imageUris.size >= 3) {
                Toast.makeText(requireContext(), "Only 3 images can be uploaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Open the gallery
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }


    private fun setupLongClickReplace() {
        firstImage.setOnLongClickListener {
            if (firstImage.drawable != null) {
                selectedImagePosition = 0
                openImagePicker()
            }
            true
        }

        secondImage.setOnLongClickListener {
            if (secondImage.drawable != null) {
                selectedImagePosition = 1
                openImagePicker()
            }
            true
        }

        thirdImage.setOnLongClickListener {
            if (thirdImage.drawable != null) {
                selectedImagePosition = 2
                openImagePicker()
            }
            true
        }

        firstImage.setOnClickListener {
            if (firstImage.drawable != null) {
                showFullScreenImage(firstImage)
            }
        }

        secondImage.setOnClickListener {
            if (secondImage.drawable != null) {
                showFullScreenImage(secondImage)
            }
        }

        thirdImage.setOnClickListener {
            if (thirdImage.drawable != null) {
                showFullScreenImage(thirdImage)
            }
        }

    }

    private fun showFullScreenImage(imageView: ImageView) {
        // Create a dialog
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        // Get the full-screen ImageView
        val fullscreenImageView: ImageView = dialog.findViewById(R.id.fullscreen_image)

        // Set the image from the clicked ImageView
        fullscreenImageView.setImageDrawable(imageView.drawable)

        // Show the dialog
        dialog.setCancelable(true)
        dialog.show()
    }



    // Open the gallery to replace the selected image
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Handle result from image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val selectedImageUri = data.data

            selectedImageUri?.let { uri ->
                if (selectedImagePosition == -1) {
                    // Add new image
                    imageUris.add(uri)
                } else {
                    // Replace existing image
                    imageUris[selectedImagePosition] = uri
                    selectedImagePosition = -1
                }

                // Display images and show the layout
                updateImages()
            }
        }
    }

    // Update the ImageViews based on the URIs in the list
    private fun updateImages() {
        if (imageUris.isNotEmpty()) {
            displayImagesLayout.visibility = View.VISIBLE
        }

        // Load the images into the respective ImageViews
        if (imageUris.size > 0) {
            Picasso.get().load(imageUris[0]).into(firstImage)
        }
        if (imageUris.size > 1) {
            Picasso.get().load(imageUris[1]).into(secondImage)
        }
        if (imageUris.size > 2) {
            Picasso.get().load(imageUris[2]).into(thirdImage)
        }

        // If 3 images are selected, show a message
        if (imageUris.size == 3) {
            Toast.makeText(requireContext(), "Only 3 images will be uploaded", Toast.LENGTH_SHORT).show()
            btnAddImages.isEnabled = false // Disable the add image button
            view?.findViewById<LinearLayout>(R.id.imagePickers)?.visibility = View.GONE
        } else {
            btnAddImages.isEnabled = true // Re-enable if less than 3
            view?.findViewById<LinearLayout>(R.id.imagePickers)?.visibility = View.VISIBLE
        }
    }


    private fun createRateTextWatcher(rateTextView: TextView): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    rateTextView.text = rate.toString() // Reset to the original rate
                    return
                }

                // Get the number of hours, ensuring it's an Int
                val hourRate = s.toString().toIntOrNull() ?: 1

                // Ensure rate is also an Int (convert if necessary)
                val currentRate = rate.toIntOrNull() ?: 0

                // Multiply the number of hours by the rate
                rateTextView.text = (hourRate * currentRate).toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }



    private fun calculateHourRate() {
        // Ensure both start and end times are not null
        if (startTimeCalendar != null && endTimeCalendar != null) {
            // If the end time is before the start time, assume it's the next day
            if (endTimeCalendar!!.before(startTimeCalendar)) {
                // Add one day to the end time
                endTimeCalendar!!.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Calculate the difference in milliseconds and convert to minutes
            val differenceInMillis = endTimeCalendar!!.timeInMillis - startTimeCalendar!!.timeInMillis
            val differenceInMinutes = (differenceInMillis / (1000 * 60)).toInt()

            // Calculate hours, rounding up if necessary
            val hours = if (differenceInMinutes % 60 == 0) {
                differenceInMinutes / 60
            } else {
                differenceInMinutes / 60 + 1
            }

            // Set the calculated hours to the EditText
            view?.findViewById<EditText>(R.id.etHourRate)?.setText(hours.toString())
        }
    }

    private fun getAddressByEmail(email: String) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val address = userSnapshot.child("address").getValue(String::class.java)
                        address?.let { locationEditText.setText(it) }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }


    private fun showStartDateTimePicker(onDateTimeSet: (Calendar) -> Unit) {
        showDatePicker { year, month, dayOfMonth ->
            showTimePicker { hour, minute ->
                 startTimeCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hour, minute)
                }
                onDateTimeSet(startTimeCalendar!!)
            }
        }
    }

    // Show End Date and Time Picker
    private fun showEndDateTimePicker(onDateTimeSet: (Calendar) -> Unit) {
        showDatePicker { year, month, dayOfMonth ->
            showTimePicker { hour, minute ->
                val endTimeCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hour, minute)
                }

                // Validate against the start time
                if (startTimeCalendar != null) {
                    if (endTimeCalendar.before(startTimeCalendar)) {
                        Toast.makeText(requireContext(), "End time cannot be earlier than start time", Toast.LENGTH_SHORT).show()
                    } else {
                        onDateTimeSet(endTimeCalendar)
                    }
                } else {
                    Toast.makeText(requireContext(), "Please select a start time first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Date Picker dialog
    private fun showDatePicker(onDateSet: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            onDateSet(year, month, dayOfMonth)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    // Time Picker dialog
    private fun showTimePicker(onTimeSet: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            onTimeSet(hourOfDay, minute)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        timePickerDialog.show()
    }



    companion object {
        @JvmStatic
        fun newInstance(bookDay: String, startTime: String, endTime: String) = ActivityFragmentClient_BookDetails().apply {
            arguments = Bundle().apply {

                putString("bookDay", bookDay)
                putString("startTime", startTime)
                putString("endTime", endTime)
            }
        }
    }
}