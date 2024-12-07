package com.capstone.peopleconnect.SPrvoider.Fragments

import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import com.capstone.peopleconnect.Notifications.model.NotificationModel
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.BookingSProviderAdapter
import com.capstone.peopleconnect.BookingDetailsFragment
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.Helper.NotificationHelper
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ActivityFragmentSProvider : Fragment(){

    private var isClickEnabled = true
    private val TAG = "ActivityFragmentSProvider"
    private lateinit var emptyView: RelativeLayout
    private lateinit var adapter: BookingSProviderAdapter
    private lateinit var recyclerView: RecyclerView
    private val allBookings = mutableListOf<Pair<String, Bookings>>()
    private var currentFilter: String = "Booking"
    private val bookings = mutableListOf<Pair<String, Bookings>>()  // Store Pair of key and booking
    private var email: String? = null
    private lateinit var notificationBadgeSProvider: TextView
    private var lastSelectedTab: String = "Booking"

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

        return inflater.inflate(R.layout.fragment_activity_s_provider, container, false)
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking_sprovider, null)
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
                        "bookingCancelProvider" to cancellationReason
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
                                        email?.let { fetchBookingsForProvider(it) }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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

        // Add after existing view initialization
        notificationBadgeSProvider = view.findViewById(R.id.notificationBadge_sprovider)
        setupNotificationBadge()

        updateDateText(view)

        // Notification icons
        val notificationIcons: ImageView = view.findViewById(R.id.notificationIcon)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()

        }

        // Message icons
        val messageIcons: ImageView = view.findViewById(R.id.messageIcon)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()

        }

        recyclerView = view.findViewById(R.id.recyclerView)
        // Replace the existing adapter initialization in onCreateView with this:
        adapter = BookingSProviderAdapter(
            bookings = bookings,
            fetchUserData = ::fetchUserData,
            onAcceptBooking = ::acceptBooking,
            onCancelBooking = ::cancelBooking,
            onItemClickListener = { bookingId ->
                // Existing code remains the same
                val bookingDetailsFragment = BookingDetailsFragment.newInstance(bookingId, isClient = false)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, bookingDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onItemLongClickListener = { bookingId, booking ->
                // Updated navigation to OngoingFragmentSProvider with correct parameters
                val ongoingFragment = OngoingFragmentSProvider.newInstance(
                    bookingId = bookingId,
                    clientEmail = booking.bookByEmail,
                    providerEmail = booking.providerEmail  // Add provider's email
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
            .load(R.drawable.nothing)  // Replace with your drawable resource or image URL
            .into(emptyImage)

        // Fetch bookings for the service provider
        email?.let { fetchBookingsForProvider(it) }

        val tvBooking = view.findViewById<TextView>(R.id.tvBooking_Present)
        val tvSuccessful = view.findViewById<TextView>(R.id.tvSuccessful_Present)
        val tvFailed = view.findViewById<TextView>(R.id.tvFailed_Present)

        // Instead, set the initial state using the custom underline:
        tvBooking.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline_provider)
        tvBooking.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
        tvBooking.typeface = ResourcesCompat.getFont(requireContext(), R.font.bold_poppins)

        // Set click listeners for each tab
        tvBooking.setOnClickListener {
            if (isClickEnabled) {
                isClickEnabled = false
                lastSelectedTab = "Booking"
                currentFilter = "Booking"
                filterBookings(currentFilter)
                highlightSelectedTab(tvBooking, tvSuccessful, tvFailed)

                view?.postDelayed({
                    isClickEnabled = true
                }, 300)
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
            notificationBadge = notificationBadgeSProvider,
            tag = TAG
        )
    }

    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())

        // Use a nullable type with `findViewById`
        val tvDate: TextView? = view.findViewById(R.id.tvDate_SPROVIDER)
        tvDate?.text = currentDate // Set the formatted date to the TextView if it's not null
    }


    private fun filterBookings(statusFilter: String) {
        // Filter based on the booking status from the second element of the pair (the Booking object)
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


    // Highlights the selected tab
    private fun highlightSelectedTab(selectedTab: TextView, vararg unselectedTabs: TextView) {
        // Set selected tab style
        when (selectedTab.text.toString()) {
            "Failed" -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline_red)
            }
            else -> {
                selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
                selectedTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.custom_underline_provider)
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

    private fun fetchBookingsForProvider(spEmail: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")

        // Query for bookings where providerEmail matches the service provider's email
        databaseReference.orderByChild("providerEmail").equalTo(spEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bookings.clear()  // Clear existing bookings in the adapter
                    allBookings.clear()  // Clear existing bookings for filtering

                    for (bookingSnapshot in snapshot.children) {
                        val booking = bookingSnapshot.getValue(Bookings::class.java)
                        val bookingKey = bookingSnapshot.key

                        if (booking != null && bookingKey != null) {
                            bookings.add(Pair(bookingKey, booking))  // Add to the adapter list
                            allBookings.add(Pair(bookingKey, booking))  // Add the booking object to the filter list
                        }
                    }

                    // Apply the current filter after fetching the data
                    filterBookings(currentFilter)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Toast.makeText(context, "Failed to fetch bookings", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun acceptBooking(bookingKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings/$bookingKey")

        // Get booking details first
        databaseReference.get().addOnSuccessListener { snapshot ->
            val booking = snapshot.getValue(Bookings::class.java)
            if (booking != null) {
                databaseReference.child("bookingStatus").setValue("Accepted")
                    .addOnSuccessListener {
                        // Send notification to client
                        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(currentUser.uid)
                                .child("name")
                                .get()
                                .addOnSuccessListener { providerNameSnapshot ->
                                    val providerName = providerNameSnapshot.getValue(String::class.java) ?: "Service Provider"

                                    // Find client's ID using their email
                                    FirebaseDatabase.getInstance().getReference("users")
                                        .orderByChild("email")
                                        .equalTo(booking.bookByEmail)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val clientId = snapshot.children.firstOrNull()?.key
                                                val clientName = snapshot.children.firstOrNull()?.child("name")?.getValue(String::class.java) ?: "Client"

                                                if (clientId != null) {
                                                    // 1. Original "Booking Accepted" notification (existing code)
                                                    val acceptanceNotification = NotificationModel(
                                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                        title = "Booking Accepted",
                                                        description = "$providerName has accepted your booking for ${booking.bookingDay}",
                                                        type = "booking",
                                                        senderId = currentUser.uid,
                                                        senderName = providerName,
                                                        timestamp = System.currentTimeMillis(),
                                                        bookingId = bookingKey,
                                                        bookingStatus = "Accepted",
                                                        bookingDate = booking.bookingDay,
                                                        bookingTime = booking.bookingStartTime
                                                    )

                                                    // 2. New ongoing progress notification for client
                                                    val clientOngoingNotification = NotificationModel(
                                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                        title = "Ongoing Service",
                                                        description = "Track your ongoing service with $providerName",
                                                        type = "ongoing",
                                                        senderId = currentUser.uid,
                                                        senderName = providerName,
                                                        timestamp = System.currentTimeMillis(),
                                                        bookingId = bookingKey,
                                                        bookingStatus = "Ongoing",
                                                        bookingDate = booking.bookingDay,
                                                        bookingTime = booking.bookingStartTime
                                                    )

                                                    // 3. New ongoing progress notification for provider
                                                    val providerOngoingNotification = NotificationModel(
                                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                        title = "Ongoing Service",
                                                        description = "Track your ongoing service with $clientName",
                                                        type = "ongoing",
                                                        senderId = clientId,
                                                        senderName = clientName,
                                                        timestamp = System.currentTimeMillis(),
                                                        bookingId = bookingKey,
                                                        bookingStatus = "Ongoing",
                                                        bookingDate = booking.bookingDay,
                                                        bookingTime = booking.bookingStartTime
                                                    )

                                                    // 4. New chat notification for client
                                                    val clientChatNotification = NotificationModel(
                                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                        title = "Chat Your Provider Now",
                                                        description = "Chat with $providerName about your booking",
                                                        type = "chat",
                                                        senderId = currentUser.uid,
                                                        senderName = providerName,
                                                        timestamp = System.currentTimeMillis(),
                                                        bookingId = bookingKey,
                                                        channelId = null  // We'll handle chat creation when clicked
                                                    )

                                                    // 5. New chat notification for provider
                                                    val providerChatNotification = NotificationModel(
                                                        id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                        title = "Chat Your Client Now",
                                                        description = "Chat with $clientName about their booking",
                                                        type = "chat",
                                                        senderId = clientId,
                                                        senderName = clientName,
                                                        timestamp = System.currentTimeMillis(),
                                                        bookingId = bookingKey,
                                                        channelId = null  // We'll handle chat creation when clicked
                                                    )

                                                    // Save all notifications
                                                    val notificationsRef = FirebaseDatabase.getInstance()
                                                        .getReference("notifications")

                                                    // Save acceptance notification to client
                                                    notificationsRef
                                                        .child(clientId)
                                                        .child(acceptanceNotification.id)
                                                        .setValue(acceptanceNotification)

                                                    // Save ongoing notification to client
                                                    notificationsRef
                                                        .child(clientId)
                                                        .child(clientOngoingNotification.id)
                                                        .setValue(clientOngoingNotification)

                                                    // Save ongoing notification to provider
                                                    notificationsRef
                                                        .child(currentUser.uid)
                                                        .child(providerOngoingNotification.id)
                                                        .setValue(providerOngoingNotification)

                                                    // Add these to the notification saving block
                                                    notificationsRef
                                                        .child(clientId)
                                                        .child(clientChatNotification.id)
                                                        .setValue(clientChatNotification)

                                                    notificationsRef
                                                        .child(currentUser.uid)
                                                        .child(providerChatNotification.id)
                                                        .setValue(providerChatNotification)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e("Notification", "Error finding client", error.toException())
                                            }
                                        })
                                }
                        }
                        Toast.makeText(context, "Booking accepted successfully", Toast.LENGTH_SHORT).show()
                        email?.let { fetchBookingsForProvider(it) }
                        updateServiceStatus(booking.providerEmail, booking.serviceOffered)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to accept booking", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }


    private fun updateServiceStatus(providerEmail: String, serviceOffered: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.orderByChild("user").equalTo(providerEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (skillSnapshot in snapshot.children) {
                        val skillItemsSnapshot = skillSnapshot.child("skillItems")

                        // Iterate over skillItems array to find the matching serviceOffered
                        for (itemSnapshot in skillItemsSnapshot.children) {
                            val name = itemSnapshot.child("name").getValue(String::class.java)
                            if (name == serviceOffered) {
                                // Update the visible field to false
                                itemSnapshot.ref.child("visible").setValue(false)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d("UpdateServiceStatus", "Visibility updated to false for $serviceOffered")
                                        } else {
                                            Log.e("UpdateServiceStatus", "Failed to update visibility")
                                        }
                                    }
                                break // Exit loop once we find the matching service
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error updating service status", error.toException())
                }
            })
    }



    private fun cancelBooking(bookingKey: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking_sprovider, null)
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

                val databaseReference = FirebaseDatabase.getInstance().getReference("bookings/$bookingKey")

                // Get booking details first
                databaseReference.get().addOnSuccessListener { snapshot ->
                    val booking = snapshot.getValue(Bookings::class.java)
                    if (booking != null) {
                        val updates = mapOf(
                            "bookingCancelProvider" to cancellationReason,
                            "bookingStatus" to "Canceled"
                        )

                        databaseReference.updateChildren(updates).addOnSuccessListener {
                            // Send notification to client
                            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(currentUser.uid)
                                    .child("name")
                                    .get()
                                    .addOnSuccessListener { providerNameSnapshot ->
                                        val providerName = providerNameSnapshot.getValue(String::class.java) ?: "Service Provider"

                                        // Find client's ID using their email
                                        FirebaseDatabase.getInstance().getReference("users")
                                            .orderByChild("email")
                                            .equalTo(booking.bookByEmail)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    val clientId = snapshot.children.firstOrNull()?.key
                                                    if (clientId != null) {
                                                        // Create cancellation notification
                                                        val notification = NotificationModel(
                                                            id = FirebaseDatabase.getInstance().reference.push().key ?: return,
                                                            title = "Booking Cancelled by Provider",
                                                            description = "$providerName has cancelled your booking for ${booking.bookingDay}",
                                                            type = "booking",
                                                            senderId = currentUser.uid,
                                                            senderName = providerName,
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
                                                            .child(clientId)
                                                            .child(notification.id)
                                                            .setValue(notification)
                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {
                                                    Log.e("Notification", "Error finding client", error.toException())
                                                }
                                            })
                                    }
                            }

                            Toast.makeText(context, "Booking canceled successfully", Toast.LENGTH_SHORT).show()
                            email?.let { fetchBookingsForProvider(it) }
                        }
                    }
                }
                alertDialog.dismiss()
            } else {
                Toast.makeText(context, "Please select a reason for canceling", Toast.LENGTH_SHORT).show()
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
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let { callback(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    companion object {
        @JvmStatic
        fun newInstance(email: String) =
            ActivityFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }

}
