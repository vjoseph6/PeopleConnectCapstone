package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.Payments
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import java.text.ParseException
import java.util.Date


class ActivityFragmentClient_BookDetails : Fragment() {

    private var serviceOffered: String = ""
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
    private  var startDateTimeTextView: String ? = null
    private  var endDateTimeTextView: String ? = null
    private lateinit var databaseReference: DatabaseReference
    private  var rate: String = "0"
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
    private var bookDay: String? = ""
    private var startTime: String? = ""
    private var endTime: String? = ""
    private var providerEmailFetched: String = ""

    private var originalRateValue: Double = 0.0
    private var isPerHourSelected = true
    private lateinit var perHourTab: TextView
    private lateinit var perTaskTab: TextView
    private lateinit var tvScope: TextView
    private lateinit var spinnerScope: Spinner
    private lateinit var etRate: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        perHourTab = view.findViewById(R.id.perHourTab)
        perTaskTab = view.findViewById(R.id.perTaskTab)
        tvScope = view.findViewById(R.id.tvScope)
        spinnerScope = view.findViewById(R.id.spinnerScope)
        etRate = view.findViewById(R.id.etRate)


        // Setup spinner options
        val scopeOptions = arrayOf(
            "Select Task Scope",
            "Small (1-2 hrs)",
            "Medium (3-5 hrs)",
            "Heavy (6 hrs+)"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_dropdown_item, // Custom layout for spinner items
            scopeOptions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item) // Apply custom layout for dropdown items
        spinnerScope.adapter = adapter

        // Initial setup
        setupPerHourView()

        // Set up click listeners
        perHourTab.setOnClickListener {
            handlePerHourTabClick()
        }

        perTaskTab.setOnClickListener {
            handlePerTaskTabClick()
        }


    }

    private fun handlePerHourTabClick() {
        if (!isPerHourSelected) {
            // Update mode to per hour
            isPerHourSelected = true

            // Get the rate EditText
            val rateEditText = view?.findViewById<EditText>(R.id.etRate)
            val hourRateEditText = view?.findViewById<EditText>(R.id.etHourRate)

            hourRateEditText?.let {
                // Set default value to 1
                it.setText("1")
                // Attach a TextWatcher to handle rate changes
                it.addTextChangedListener(rateEditText?.let { it1 -> createRateTextWatcher(it1) })
            }

            // Disable rate EditText
            rateEditText?.isEnabled = false

            // Hide task scope views
            view?.findViewById<TextView>(R.id.tvScope)?.visibility = View.GONE
            view?.findViewById<Spinner>(R.id.spinnerScope)?.visibility = View.GONE

            // Highlight selected tab
            highlightSelectedTab(perHourTab, perTaskTab)
        }
    }


    private fun handlePerTaskTabClick() {
        if (isPerHourSelected) {
            // Switch to per task mode
            isPerHourSelected = false

            // Enable the etRate EditText and set it to display only a hint
            etRate.isEnabled = true
            etRate.setText("") // Clear any existing value
            etRate.hint = "Enter task rate" // Set hint text (customize as needed)

            // Show task scope views
            tvScope.visibility = View.VISIBLE
            spinnerScope.visibility = View.VISIBLE

            // Reset spinner to first item
            spinnerScope.setSelection(0)

            // Highlight selected tab
            highlightSelectedTab(perTaskTab, perHourTab)
        }
    }


    private fun setupPerHourView() {
        isPerHourSelected = true
        etRate.setText(originalRateValue.toString())
        etRate.isEnabled = false

        // Hide task scope views
        tvScope.visibility = View.GONE
        spinnerScope.visibility = View.GONE

        // Highlight per hour tab
        highlightSelectedTab(perHourTab, perTaskTab)
    }

    private fun highlightSelectedTab(selectedTab: TextView, vararg unselectedTabs: TextView) {
        // Set selected tab style based on the tab text
        when (selectedTab.text.toString()) {
            "Hour Based" -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline)
            }

            "Task Based" -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline)
            }

            else -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline)
            }
        }

        selectedTab.typeface = ResourcesCompat.getFont(requireContext(), R.font.bold_poppins)
        selectedTab.paintFlags = selectedTab.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()

        // Set unselected tabs style
        unselectedTabs.forEach { tab ->
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            tab.typeface = ResourcesCompat.getFont(requireContext(), R.font.bold_poppins)
            tab.background = null
            tab.paintFlags = tab.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        // Initialize StripeHelper in onCreate instead of onCreateView
        stripeHelper = StripeHelper(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity_client__book_details, container, false)

        serviceOffered = arguments?.getString("SERVICE_OFFERED") ?: ""

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


        bookNow = view.findViewById(R.id.btnBookNow)
        bookNow.setOnClickListener {
            initiatePayment()
        }


        //for the upload image
        displayImagesLayout = view.findViewById(R.id.displayImages)
        firstImage = view.findViewById(R.id.firstImage)
        secondImage = view.findViewById(R.id.secondImage)
        thirdImage = view.findViewById(R.id.thirdImage)

        setupImagePicker()
        setupLongClickReplace()


        // Get address by email
        val email = auth.currentUser?.email ?: ""
        email?.let { getAddressByEmail(it) }

        val btnBackClient: ImageButton = view.findViewById(R.id.btnBackClient)
        btnBackClient.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }


        val rateTextView = view.findViewById<EditText>(R.id.etRate)

        // Set default hour rate
        val hourRateEditText = view.findViewById<EditText>(R.id.etHourRate)
        hourRateEditText.setText("1")
        hourRateEditText.addTextChangedListener(createRateTextWatcher(rateTextView))

        fetchSkillRate()

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)

        dateEditText.setText(formattedDate)


        // Date and time pickers
        dateIcon.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                val calendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                bookDay = formattedDate // Update the stored bookDay
                // Clear times when date changes
                startTimeEditText.text.clear()
                endTimeEditText.text.clear()
                startTimeCalendar = null
                endTimeCalendar = null
            }
        }

        startIcon.setOnClickListener {
            showStartDateTimePicker { calendar ->
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                startTimeEditText.setText(timeFormat.format(calendar.time))
                startTimeCalendar = calendar
                startTime = timeFormat.format(calendar.time) // Update stored startTime

                // Clear end time if it's now invalid
                if (endTimeCalendar != null && endTimeCalendar!!.before(startTimeCalendar)) {
                    endTimeEditText.text.clear()
                    endTimeCalendar = null
                    endTime = null
                }
                calculateHourRate()
            }
        }

        endIcon.setOnClickListener {
            if (startTimeEditText.text.isEmpty()) {
                Toast.makeText(context, "Please select start time first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showEndDateTimePicker { calendar ->
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                endTimeEditText.setText(timeFormat.format(calendar.time))
                endTimeCalendar = calendar
                endTime = timeFormat.format(calendar.time) // Update stored endTime
                calculateHourRate()
            }
        }

        // Make EditTexts non-editable but clickable
        dateEditText.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { dateIcon.performClick() }
        }

        startTimeEditText.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { startIcon.performClick() }
        }

        endTimeEditText.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { endIcon.performClick() }
        }

        return view
    }


    private fun showDatePicker(onDateSet: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()

        // Use existing bookDay if available, otherwise use current date
        if (!bookDay.isNullOrEmpty()) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDate = LocalDate.parse(bookDay, formatter)
                calendar.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
            } catch (e: Exception) {
                Log.e("DatePicker", "Error parsing bookDay: $bookDay", e)
            }
        }

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
        showDatePicker { year, month, dayOfMonth ->
            var initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            var initialMinute = Calendar.getInstance().get(Calendar.MINUTE)

            if (!startTime.isNullOrEmpty()) {
                try {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val parsedTime = timeFormat.parse(startTime)
                    val timeCalendar = Calendar.getInstance().apply {
                        time = parsedTime
                    }
                    initialHour = timeCalendar.get(Calendar.HOUR_OF_DAY)
                    initialMinute = timeCalendar.get(Calendar.MINUTE)
                } catch (e: Exception) {
                    Log.e("TimePicker", "Error parsing startTime: $startTime", e)
                }
            }

            showTimePicker(year, month, dayOfMonth, initialHour, initialMinute) { hour, minute ->
                val selectedDateTime = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hour, minute)
                }

                val dateTimeFormatter = SimpleDateFormat("MM/dd/yyyy - hh:mm a", Locale.getDefault())
                startDateTimeTextView = dateTimeFormatter.format(selectedDateTime.time)

                onDateTimeSet(selectedDateTime)
            }
        }
    }

    private fun showEndDateTimePicker(onDateTimeSet: (Calendar) -> Unit) {
        if (startTimeCalendar == null) {
            Toast.makeText(requireContext(), "Please select start time first", Toast.LENGTH_SHORT).show()
            return
        }

        showDatePicker { year, month, dayOfMonth ->
            showTimePicker(year, month, dayOfMonth,
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE)
            ) { hour, minute ->
                val selectedEndCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hour, minute)
                }

                // Check if the selected end time is exactly the same as start time
                if (selectedEndCalendar.get(Calendar.YEAR) == startTimeCalendar!!.get(Calendar.YEAR) &&
                    selectedEndCalendar.get(Calendar.MONTH) == startTimeCalendar!!.get(Calendar.MONTH) &&
                    selectedEndCalendar.get(Calendar.DAY_OF_MONTH) == startTimeCalendar!!.get(Calendar.DAY_OF_MONTH) &&
                    selectedEndCalendar.get(Calendar.HOUR_OF_DAY) == startTimeCalendar!!.get(Calendar.HOUR_OF_DAY) &&
                    selectedEndCalendar.get(Calendar.MINUTE) == startTimeCalendar!!.get(Calendar.MINUTE)
                ) {
                    Toast.makeText(requireContext(), "End time cannot be the same as start time", Toast.LENGTH_SHORT).show()
                    return@showTimePicker
                }

                // If end time is before start time
                if (selectedEndCalendar.before(startTimeCalendar)) {
                    Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
                    return@showTimePicker
                }

                val dateTimeFormatter = SimpleDateFormat("MM/dd/yyyy - hh:mm a", Locale.getDefault())
                endDateTimeTextView = dateTimeFormatter.format(selectedEndCalendar.time)
                endTimeCalendar = selectedEndCalendar
                onDateTimeSet(endTimeCalendar!!)
            }
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

    private fun initiatePayment() {
        val userEmail = auth.currentUser?.email ?: ""
        val serviceOffered = arguments?.getString("SERVICE_OFFERED") ?: ""
        val currency = "php"
        val providerEmail = providerEmailFetched
        val bookingStartTime = startTimeEditText.text.toString()
        val bookingEndTime = endTimeEditText.text.toString()
        val bookingDay = dateEditText.text.toString()
        val bookingAmountString = view?.findViewById<EditText>(R.id.etRate)?.text?.toString()
        val bookingAmount = bookingAmountString?.toDoubleOrNull() ?: 0.0 // Fallback to 0.0 if the conversion fails

        // Validate input fields
        if (providerEmail.isEmpty() ||
            serviceOffered.isEmpty() ||
            startTimeEditText.text.isEmpty() ||
            endTimeEditText.text.isEmpty() ||
            descEditText.text.isEmpty() ||
            dateEditText.text.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all the required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for duplicate booking first
        checkForDuplicateBooking(userEmail, providerEmail, bookingDay, bookingStartTime, bookingEndTime, serviceOffered.toString()) { duplicateFound ->
            if (duplicateFound) {
                requireContext().let {
                    dismissLoadingDialog()
                    Toast.makeText(it, "You already have a pending booking with this provider for this service.", Toast.LENGTH_SHORT).show()
                }
            }else {
                // Proceed to initiate payment
                showLoadingDialog()
                stripeHelper.fetchPayment(bookingAmount, currency, userEmail, providerEmail, serviceOffered)
            }
        }
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


    private fun checkForDuplicateBooking(
        userEmail: String,
        providerEmail: String,
        bookingDay: String,
        bookingStartTime: String,
        bookingEndTime: String,
        serviceOffered: String,
        onResult: (Boolean) -> Unit
    ) {
        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")
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
                            val existingBookingDay = existingBooking.bookingDay
                            val existingStartMinutes = convertToMinutesSinceMidnight(existingBooking.bookingStartTime)
                            val existingEndMinutes = convertToMinutesSinceMidnight(existingBooking.bookingEndTime)

                            // Check for same-day overlapping bookings
                            if (bookingDay == existingBookingDay &&
                                isTimeOverlap(
                                    currentBookingStartMinutes, currentBookingEndMinutes,
                                    existingStartMinutes, existingEndMinutes
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
                    Toast.makeText(
                        requireContext(),
                        "Error checking for duplicate: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
        // Handle cross-day bookings
        val currentEndAdjusted = if (currentStart > currentEnd) currentEnd + 1440 else currentEnd
        val existingEndAdjusted = if (existingStart > existingEnd) existingEnd + 1440 else existingEnd

        // Check for overlap
        return currentStart < existingEndAdjusted && currentEndAdjusted > existingStart
    }

    private fun convertToMinutesSinceMidnight(timeString: String): Int {
        if (timeString.isEmpty()) {
            Log.e(TAG, "Time string is empty, cannot convert to minutes.")
            return -1
        }

        val formats = arrayOf(
            "MM/dd/yyyy - hh:mm a",
            "hh:mm a",
            "h:mm a"
        )

        for (formatString in formats) {
            try {
                val dateFormat = SimpleDateFormat(formatString, Locale.getDefault())
                val date = dateFormat.parse(timeString)
                val calendar = Calendar.getInstance()
                calendar.time = date
                val hours = calendar.get(Calendar.HOUR_OF_DAY)
                val minutes = calendar.get(Calendar.MINUTE)
                return hours * 60 + minutes
            } catch (e: ParseException) {
                // Continue to next format if parsing fails
                continue
            }
        }

        Log.e(TAG, "Unable to parse time string: $timeString")
        return -1
    }



    fun saveBooking(
        originalAmount: Double = 0.0,
        commissionAmount: Double = 0.0,
        totalAmount: Double = 0.0,
        paymentId: String,
        paymentMethod: String,
        paymentDate: String
    ) {
        dismissLoadingDialog()
        showLoadingDialog()

        userEmail = auth.currentUser?.email ?: ""
        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")
        val paymentReference = FirebaseDatabase.getInstance().getReference("payments")
        val bookingId = bookingReference.push().key ?: return

        val payment = Payments(
            paymentId = paymentId,
            paymentMethod = paymentMethod,
            bookBy = userEmail.toString(),
            providerEmail = providerEmailFetched,
            paymentAmount = totalAmount,
            paymentDate = paymentDate,
            bookingId = bookingId,
            originalAmount = originalAmount,
            commissionAmount = commissionAmount
        )

        // Save payment details
        paymentReference.child(paymentId).setValue(payment)
            .addOnSuccessListener {
                Log.d("Payment", "Payment details saved successfully")
                // Proceed with saving booking details
                proceedWithBookingSave(
                    bookingId,
                    payment,
                    userEmail.toString(),
                    providerEmailFetched
                )
            }
            .addOnFailureListener { e ->
                Log.e("Payment", "Error saving payment details", e)
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Failed to save payment details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun proceedWithBookingSave(
        bookingId: String,
        payment: Payments,
        userEmail: String,
        providerEmail: String
    ) {
        val validImageUris = imageUris.filterNotNull()
        val selectedScope = spinnerScope.selectedItem.toString()

        uploadImagesToFirebase(userEmail, validImageUris, bookingId) { imageUrls ->
            val booking = Bookings(
                bookingId = bookingId,
                bookByEmail = userEmail,
                providerEmail = providerEmail,
                bookingStatus = "Pending",
                serviceOffered = serviceOffered,
                bookingStartTime = startDateTimeTextView.toString(),
                bookingEndTime = endDateTimeTextView.toString(),
                bookingDescription = descEditText.text.toString(),
                bookingDay = dateEditText.text.toString(),
                bookingLocation = locationEditText.text.toString(),
                bookingAmount = payment.originalAmount,
                bookingScope = selectedScope,
                bookingCommissionAmount = payment.commissionAmount,
                bookingTotalAmount = payment.paymentAmount,
                bookingPaymentMethod = payment.paymentMethod,
                bookingPaymentId = payment.paymentId,
                bookingCancelClient = "",
                bookingCancelProvider = "",
                bookingUploadImages = imageUrls
            )

            // Establish chat connection regardless of image upload status
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val databaseRef = FirebaseDatabase.getInstance().getReference("chat_connections")

            if (currentUserId != null) {
                FirebaseDatabase.getInstance().getReference("users")
                    .orderByChild("email")
                    .equalTo(providerEmail)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val providerId = snapshot.children.firstOrNull()?.child("userId")?.getValue(String::class.java)
                            if (providerId != null) {
                                databaseRef.child(currentUserId).child(providerId).setValue(true)
                                databaseRef.child(providerId).child(currentUserId).setValue(true)
                            }

                            // Save booking after establishing chat connection
                            if (imageUrls.isNotEmpty() || validImageUris.isEmpty()) {
                                saveBookingToDatabase(bookingId, booking)
                            } else {
                                Log.e("SaveBooking", "Image upload failed.")
                                Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                                dismissLoadingDialog()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Booking", "Failed to establish chat connection", error.toException())
                            // Still save the booking even if chat connection fails
                            if (imageUrls.isNotEmpty() || validImageUris.isEmpty()) {
                                saveBookingToDatabase(bookingId, booking)
                            } else {
                                Log.e("SaveBooking", "Image upload failed.")
                                Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                                dismissLoadingDialog()
                            }
                        }
                    })
            }
        }
    }

    private fun saveBookingToDatabase(bookingId: String, booking: Bookings) {
        val bookingReference = FirebaseDatabase.getInstance().getReference("bookings")
        bookingReference.child(bookingId).setValue(booking)
            .addOnSuccessListener {
                // Send notification to provider
                sendBookingNotificationToProvider(booking)

                dismissLoadingDialog()

                // Navigate back to ActivityFragmentClient
                val activityFragment = ActivityFragmentClient.newInstance(userEmail ?: "")
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, activityFragment)
                    .commit()
            }
            .addOnFailureListener { e ->
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Failed to save booking", Toast.LENGTH_SHORT).show()
                Log.e("SaveBooking", "Error saving booking", e)
            }
    }


    private fun sendBookingNotificationToProvider(booking: Bookings) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.uid)
                .child("name")
                .get()
                .addOnSuccessListener { clientNameSnapshot ->
                    val clientName = clientNameSnapshot.getValue(String::class.java) ?: "A client"

                    // Find provider's ID using their email
                    FirebaseDatabase.getInstance().getReference("users")
                        .orderByChild("email")
                        .equalTo(booking.providerEmail)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val providerId = snapshot.children.firstOrNull()?.key
                                if (providerId != null) {
                                    // Create booking notification
                                    val notification = NotificationModel(
                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                        title = "New Booking Request",
                                        description = "$clientName has requested a booking for ${booking.bookingDay}",
                                        type = "booking",
                                        senderId = currentUser.uid,
                                        senderName = clientName,
                                        timestamp = System.currentTimeMillis(),
                                        bookingId = booking.bookingId,  // Make sure this is set
                                        bookingStatus = "Pending",
                                        bookingDate = booking.bookingDay,
                                        bookingTime = booking.bookingStartTime
                                    )

                                    // Save notification to Firebase
                                    FirebaseDatabase.getInstance()
                                        .getReference("notifications")
                                        .child(providerId)
                                        .child(notification.id)
                                        .setValue(notification)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Notification", "Error finding provider", error.toException())
                            }
                        })
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
        val fullscreenImageView: ImageView = dialog.findViewById(R.id.fullscreenImageView)

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

    private fun updateRateDisplay() {
        val rateEditText = view?.findViewById<EditText>(R.id.etRate)
        val hourRateEditText = view?.findViewById<EditText>(R.id.etHourRate)

        val hours = hourRateEditText?.text?.toString()?.toIntOrNull() ?: 1
        val baseRate = rate.toIntOrNull() ?: 0
        val total = hours * baseRate

        Log.d("RateCalculation", "Hours: $hours, BaseRate: $baseRate, Total: $total")

        // Update the total rate display with whole number
        rateEditText?.setText(total.toString())
    }





    private fun calculateHourRate() {
        if (startTimeCalendar != null && endTimeCalendar != null) {
            var endCalendar = endTimeCalendar!!.clone() as Calendar

            // If end time is before start time, assume it's next day
            if (endCalendar.before(startTimeCalendar)) {
                endCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Calculate time difference
            val differenceInMillis = endCalendar.timeInMillis - startTimeCalendar!!.timeInMillis
            val differenceInMinutes = (differenceInMillis / (1000 * 60)).toInt()

            // Calculate hours, rounding up for partial hours
            val hours = ceil(differenceInMinutes / 60.0).toInt()

            // Update hour rate display
            view?.findViewById<EditText>(R.id.etHourRate)?.setText(hours.toString())

            // Update the total rate based on hours and base rate
            updateRateDisplay()
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

    private fun fetchSkillRate() {
        val providerName = arguments?.getString("NAME") ?: ""
        val currentService = arguments?.getString("SERVICE_OFFERED") ?: ""

        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("name").equalTo(providerName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("SkillRate", "Users snapshot exists: ${snapshot.exists()}")
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            providerEmailFetched = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            if (providerEmailFetched.isNotEmpty()) {
                                fetchSkillRateWithEmail(providerEmailFetched, currentService)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SkillRate", "Error fetching provider email: ${error.message}")
                }
            })
    }

    private fun fetchSkillRateWithEmail(providerEmail: String, currentService: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (skillSetSnapshot in snapshot.children) {
                    val userEmail = skillSetSnapshot.child("user").getValue(String::class.java)
                    Log.d("SkillRate", "Checking user email: $userEmail vs $providerEmail")

                    if (userEmail == providerEmail) {
                        val skillItems = skillSetSnapshot.child("skillItems")

                        for (skillItem in skillItems.children) {
                            val name = skillItem.child("name").getValue(String::class.java)
                            val skillRate = skillItem.child("skillRate").getValue(Float::class.java)
                            Log.d("SkillRate", "Checking skill: $name with rate: $skillRate")

                            if (name == currentService) {
                                // Convert to whole number and ensure it's not null
                                val wholeRate = skillRate?.toInt() ?: 0
                                rate = wholeRate.toString()

                                Log.d("SkillRate", "Setting rate to: $rate")

                                // Ensure we're on the main thread when updating UI
                                activity?.runOnUiThread {
                                    view?.findViewById<EditText>(R.id.etRate)?.setText(rate)
                                    calculateHourRate()
                                    updateRateDisplay()
                                }
                                break
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SkillRate", "Error fetching skill rate: ${error.message}")
            }
        })
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