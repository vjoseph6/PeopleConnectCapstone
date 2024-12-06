package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.BookingClientAdapter
import com.capstone.peopleconnect.BookingDetailsFragment
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.os.Handler
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.capstone.peopleconnect.Helper.NotificationHelper


class ActivityFragmentClient : Fragment() {

    private var isClickEnabled = true
    private lateinit var emptyView: RelativeLayout
    private lateinit var adapter: BookingClientAdapter
    private lateinit var recyclerView: RecyclerView
    private val bookings = mutableListOf<Pair<String, Bookings>>()  // Store Pair of key and booking
    private var email: String? = null
    private val allBookings = mutableListOf<Pair<String, Bookings>>()// Store all bookings for filtering
    private var currentFilter: String = "Booking"  // Track the current tab/filter
    private lateinit var notificationBadge: TextView
    private var lastSelectedTab: String = "Booking"  // Track the last selected tab

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
        val view = inflater.inflate(R.layout.fragment_activity_client, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        // Replace the existing adapter initialization in onCreateView with this:
        adapter = BookingClientAdapter(
            bookings = bookings,
            fetchUserData = ::fetchUserData,
            onCancelBooking = ::cancelBooking,
            onItemClickListener = { bookingId ->
                // Navigate to BookingDetailsFragment with the bookingId and isClient flag
                val bookingDetailsFragment = BookingDetailsFragment.newInstance(bookingId, isClient = true)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, bookingDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onItemLongClickListener = { bookingId, booking ->
                // Navigate to OngoingFragmentClient
                val ongoingFragment = OngoingFragmentClient.newInstance(
                    bookingId = bookingId,
                    providerEmail = booking.providerEmail,
                    clientEmail = booking.bookByEmail  // Add this parameter
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, ongoingFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        emptyView = view.findViewById(R.id.emptyView)

        // Load image into ImageView using Glide
        val emptyImage: ImageView = view.findViewById(R.id.image)
        Glide.with(this)
            .load(R.drawable.nothing)
            .into(emptyImage)

        // Fetch bookings for the client
        email?.let { fetchBookingsForClient(it) }

        val tvBooking = view.findViewById<TextView>(R.id.tvBooking_Present)
        val tvSuccessful = view.findViewById<TextView>(R.id.tvSuccessful_Present)
        val tvFailed = view.findViewById<TextView>(R.id.tvFailed_Present)

        // Instead, set the initial state using the custom underline:
        tvBooking.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline)
        tvBooking.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        tvBooking.typeface = ResourcesCompat.getFont(requireContext(), R.font.bold_poppins)

        // Set click listeners for each tab
        tvBooking.setOnClickListener {
            if (isClickEnabled) {
                isClickEnabled = false
                lastSelectedTab = "Booking"
                currentFilter = "Booking"
                filterBookings(currentFilter)
                highlightSelectedTab(tvBooking, tvSuccessful, tvFailed)

                // Re-enable clicks after a short delay
                view?.postDelayed({
                    isClickEnabled = true
                }, 300) // 300ms delay
            }
        }
        tvSuccessful.setOnClickListener {
            if (isClickEnabled) {
                isClickEnabled = false
                lastSelectedTab = "Successful"
                currentFilter = "Successful"
                filterBookings(currentFilter)
                highlightSelectedTab(tvSuccessful, tvBooking, tvFailed)

                view?.postDelayed({
                    isClickEnabled = true
                }, 300)
            }
        }

        tvFailed.setOnClickListener {
            if (isClickEnabled) {
                isClickEnabled = false
                lastSelectedTab = "Failed"
                currentFilter = "Failed"
                filterBookings(currentFilter)
                highlightSelectedTab(tvFailed, tvBooking, tvSuccessful)

                view?.postDelayed({
                    isClickEnabled = true
                }, 300)
            }
        }

        // Add this to restore the last selected tab
        when (lastSelectedTab) {
            "Successful" -> tvSuccessful.performClick()
            "Failed" -> tvFailed.performClick()
            "Booking" -> tvBooking.performClick()
            else -> tvBooking.performClick()  // Default to Booking tab
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Add after existing view initialization
        notificationBadge = view.findViewById(R.id.notificationBadge)
        setupNotificationBadge()


        updateDateText(view)

        // Check if we need to auto-cancel bookings
        // Check if we need to auto-cancel bookings
        arguments?.let { args ->
            val target = args.getString("target")
            val serviceType = args.getString("serviceType")
            val intent = args.getString("intent")

            // First, check the intent
            if (intent == "cancel_booking") {
                val tvBooking = view.findViewById<TextView>(R.id.tvBooking_Present)
                tvBooking.performClick()

                Handler().postDelayed({
                    // If intent matches, proceed with cancellation
                    if (serviceType.isNullOrEmpty() || serviceType == "Service Type not found") {
                        // Cancel all pending bookings
                        cancelAllPendingBookings()
                    } else {
                        // Cancel specific service type bookings
                        cancelBookingsByService(serviceType)
                    }
                }, 500)
            }
            // If intent doesn't match, check target as a fallback
            else if (target == "cancel_booking") {
                val tvBooking = view.findViewById<TextView>(R.id.tvBooking_Present)
                tvBooking.performClick()

                Handler().postDelayed({
                    if (serviceType.isNullOrEmpty() || serviceType == "Service Type not found") {
                        // Cancel all pending bookings
                        cancelAllPendingBookings()
                    } else {
                        // Cancel specific service type bookings
                        cancelBookingsByService(serviceType)
                    }
                }, 500)
            } else {

            }
        }

        // Notification icons
        val notificationIcons: LinearLayout = view.findViewById(R.id.notificationLayout)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()

        }

        // Message icons
        val messageIcons: LinearLayout = view.findViewById(R.id.messageLayout)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()

        }


    }

    override fun onResume() {
        super.onResume()
        // Restore the last selected tab state when returning to the fragment
        view?.let { view ->
            val tvBooking = view.findViewById<TextView>(R.id.tvBooking_Present)
            val tvSuccessful = view.findViewById<TextView>(R.id.tvSuccessful_Present)
            val tvFailed = view.findViewById<TextView>(R.id.tvFailed_Present)

            when (lastSelectedTab) {
                "Successful" -> tvSuccessful.performClick()
                "Failed" -> tvFailed.performClick()
                "Booking" -> tvBooking.performClick()
                else -> tvBooking.performClick()  // Default to Booking tab
            }
        }
    }

    private fun setupNotificationBadge() {
        NotificationHelper.setupNotificationBadge(
            fragment = this,
            notificationBadge = notificationBadge,
            tag = ContentValues.TAG
        )
    }

    // Filters bookings based on status
    private fun filterBookings(statusFilter: String) {
        val filteredBookings = when (statusFilter) {
            "Booking" -> allBookings.filter { it.second.bookingStatus == "Pending" }
            "Successful" -> allBookings.filter { it.second.bookingStatus == "Accepted"}
            "Failed" -> allBookings.filter { it.second.bookingStatus == "Canceled" || it.second.bookingStatus == "Complete" || it.second.bookingStatus == "Completed"}
            else -> allBookings
        }

        adapter.updateBookings(filteredBookings)

        // Handle visibility of empty view
        if (filteredBookings.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())

        // Find the TextView and set the formatted date
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        tvDate.text = currentDate // Set the formatted date to the TextView
    }

    // Highlights the selected tab
    private fun highlightSelectedTab(selectedTab: TextView, vararg unselectedTabs: TextView) {
        // Set selected tab style
        when (selectedTab.text.toString()) {
            "Failed" -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline_red)
            }
            else -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
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


    // Fetch bookings for the client
    private fun fetchBookingsForClient(clientEmail: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
        databaseReference.orderByChild("bookByEmail").equalTo(clientEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bookings.clear()
                    allBookings.clear()

                    for (bookingSnapshot in snapshot.children) {
                        val booking = bookingSnapshot.getValue(Bookings::class.java)
                        val bookingKey = bookingSnapshot.key
                        if (booking != null && bookingKey != null) {
                            bookings.add(Pair(bookingKey, booking))
                            allBookings.add(Pair(bookingKey, booking)) // Save to allBookings for filtering
                        }
                    }

                    // Automatically filter based on the current selected tab
                    filterBookings(currentFilter)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun cancelBooking(bookingKey: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = builder.create()
        val btnYes: Button = dialogView.findViewById(R.id.btnYes)
        val btnNo: TextView = dialogView.findViewById(R.id.btnNo)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.optionsRadioGroup)

        btnYes.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            if (selectedOptionId != -1) {
                val selectedRadioButton: RadioButton = dialogView.findViewById(selectedOptionId)
                val cancellationReason = selectedRadioButton.text.toString()

                // Get the booking details first
                FirebaseDatabase.getInstance().getReference("bookings")
                    .child(bookingKey)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val booking = snapshot.getValue(Bookings::class.java)
                        if (booking != null) {
                            // Update booking status
                            val updates = mapOf(
                                "bookingCancelClient" to cancellationReason,
                                "bookingStatus" to "Canceled"
                            )

                            snapshot.ref.updateChildren(updates).addOnSuccessListener {
                                // Send notification to provider
                                FirebaseAuth.getInstance().currentUser?.let { currentUser ->
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
                                                            // Create cancellation notification
                                                            val notification = NotificationModel(
                                                                id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                                title = "Booking Cancelled",
                                                                description = "$clientName has cancelled the booking for ${booking.bookingDay}",
                                                                type = "booking",
                                                                senderId = currentUser.uid,
                                                                senderName = clientName,
                                                                timestamp = System.currentTimeMillis(),
                                                                bookingId = bookingKey,
                                                                bookingStatus = "Cancelled",
                                                                bookingDate = booking.bookingDay,
                                                                bookingTime = booking.bookingStartTime,
                                                                cancellationReason = cancellationReason
                                                            )

                                                            // Save notification
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

                                Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                                email?.let { fetchBookingsForClient(it) }
                            }
                        }
                    }
                alertDialog.dismiss()
            } else {
                Toast.makeText(context, "Please select a reason for cancelling", Toast.LENGTH_SHORT).show()
            }
        }

        btnNo.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        alertDialog.show()
    }

    private fun fetchUserData(providerEmail: String, callback: (User) -> Unit) {
        val userReference = FirebaseDatabase.getInstance().getReference("users")

        userReference.orderByChild("email").equalTo(providerEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let { callback(it) }  // Pass user data back to the adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun cancelAllPendingBookings() {
        val pendingBookings = allBookings.filter {
            it.second.bookingStatus != "Canceled" &&
            it.second.bookingStatus != "Completed" &&
            it.second.bookingStatus != "COMPLETED" &&
            it.second.bookingStatus != "COMPLETE" &&
            it.second.bookingStatus != "Failed" &&
            it.second.bookingStatus != "Accepted"
        }

        if (pendingBookings.isEmpty()) {
            Toast.makeText(context, "No pending bookings to cancel", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.client_dialog_logout, null)
        val tvTitle: TextView = dialogView.findViewById(R.id.tvLogoutTitle)
        val btnDelete: Button = dialogView.findViewById(R.id.btnLogout)
        val tvCancel: TextView = dialogView.findViewById(R.id.tvCancel)

        // Customize dialog text for delete confirmation
        tvTitle.text = " Are you sure you want to cancel all pending bookings?"
        btnDelete.text = "Yes, cancel it"

        // Create and display the AlertDialog
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        alertDialog.show()

        tvCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnDelete.setOnClickListener {
            showCancellationDialog(pendingBookings)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun cancelBookingsByService(targetServiceType: String) {
        val pendingBookings = allBookings.filter {
            it.second.bookingStatus != "Canceled" &&
                    it.second.bookingStatus != "Completed" &&
                    it.second.bookingStatus != "COMPLETED" &&
                    it.second.bookingStatus != "COMPLETE" &&
                    it.second.bookingStatus != "Failed" &&
                    it.second.bookingStatus != "Accepted" &&
                    it.second.serviceOffered == targetServiceType
        }

        if (pendingBookings.isEmpty()) {
            Toast.makeText(
                context,
                "No pending bookings found for $targetServiceType",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.client_dialog_logout, null)
        val tvTitle: TextView = dialogView.findViewById(R.id.tvLogoutTitle)
        val btnDelete: Button = dialogView.findViewById(R.id.btnLogout)
        val tvCancel: TextView = dialogView.findViewById(R.id.tvCancel)

        // Customize dialog text for delete confirmation
        tvTitle.text = " Are you sure you want to cancel all pending bookings for $targetServiceType?"
        btnDelete.text = "Yes, cancel it"

        // Create and display the AlertDialog
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        alertDialog.show()

        tvCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()

        btnDelete.setOnClickListener {
            showCancellationDialog(pendingBookings)
            alertDialog.dismiss()
        }

    }

    private fun showCancellationDialog(bookingsToCancel: List<Pair<String, Bookings>>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = builder.create()
        val btnYes: Button = dialogView.findViewById(R.id.btnYes)
        val btnNo: TextView = dialogView.findViewById(R.id.btnNo)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.optionsRadioGroup)


        btnYes.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            if (selectedOptionId != -1) {
                val selectedRadioButton: RadioButton = dialogView.findViewById(selectedOptionId)
                val cancellationReason = selectedRadioButton.text.toString()

                var successCount = 0
                bookingsToCancel.forEach { (bookingKey, _) ->
                    val databaseReference = FirebaseDatabase.getInstance()
                        .getReference("bookings/$bookingKey")

                    val updates = mapOf(
                        "bookingCancelClient" to cancellationReason
                    )

                    databaseReference.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            databaseReference.removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    successCount++
                                    if (successCount == bookingsToCancel.size) {
                                        Toast.makeText(
                                            context,
                                            "Successfully canceled ${bookingsToCancel.size} booking(s)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        email?.let { fetchBookingsForClient(it) }
                                    }
                                }
                            }
                        }
                    }
                }
                alertDialog.dismiss()
            } else {
                Toast.makeText(
                    context,
                    "Please select a reason for canceling",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnNo.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        alertDialog.show()
    }

    companion object {
        @JvmStatic
        fun newInstance(email: String) =
            ActivityFragmentClient().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}

