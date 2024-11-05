package com.capstone.peopleconnect.SPrvoider.Fragments

import android.app.AlertDialog
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Adapters.BookingSProviderAdapter
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ActivityFragmentSProvider : Fragment(){

    private lateinit var emptyView: RelativeLayout
    private lateinit var adapter: BookingSProviderAdapter
    private lateinit var recyclerView: RecyclerView
    private val allBookings = mutableListOf<Pair<String, Bookings>>()
    private var currentFilter: String = "Booking"
    private val bookings = mutableListOf<Pair<String, Bookings>>()  // Store Pair of key and booking
    private var email: String? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        adapter = BookingSProviderAdapter(bookings, ::fetchUserData, ::acceptBooking, ::cancelBooking)
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

        // Set click listeners for each tab
        tvBooking.setOnClickListener {
            currentFilter = "Booking"  // Update current filter
            filterBookings(currentFilter)
            highlightSelectedTab(tvBooking, tvSuccessful, tvFailed)
        }

        tvSuccessful.setOnClickListener {
            currentFilter = "Successful"  // Update current filter
            filterBookings(currentFilter)
            highlightSelectedTab(tvSuccessful, tvBooking, tvFailed)
        }

        tvFailed.setOnClickListener {
            currentFilter = "Failed"  // Update current filter
            filterBookings(currentFilter)
            highlightSelectedTab(tvFailed, tvBooking, tvSuccessful)
        }
    }

    private fun filterBookings(statusFilter: String) {
        // Filter based on the booking status from the second element of the pair (the Booking object)
        val filteredBookings = when (statusFilter) {
            "Booking" -> allBookings.filter { it.second.bookingStatus != "Canceled" && it.second.bookingStatus != "Completed" }
            "Successful" -> allBookings.filter { it.second.bookingStatus == "Completed" }
            "Failed" -> allBookings.filter { it.second.bookingStatus == "Canceled" }
            else -> allBookings
        }

        // The pairedBookings will remain as List<Pair<String, Bookings>>
        // filteredBookings already contains pairs, so no need to map them again
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
    private fun highlightSelectedTab(selectedTab: TextView, vararg otherTabs: TextView) {
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
        otherTabs.forEach { it.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)) }
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
                        Log.d("INATAY KA, ","$bookingKey")

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
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")

        bookingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the children to find the matching bookingKey
                for (bookingSnapshot in snapshot.children) {
                    val currentKey = bookingSnapshot.key

                    // Check if the current key matches the bookingKey passed
                    if (currentKey == bookingKey) {
                        // Prepare the updates for the booking
                        val updates = mapOf(
                            "bookingStatus" to "Accepted"  // Update the status to Accepted
                        )

                        // Update the booking status
                        bookingSnapshot.ref.updateChildren(updates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Optionally refresh the bookings list after accepting
                                Toast.makeText(context, "Booking accepted successfully", Toast.LENGTH_SHORT).show()
                                fetchBookingsForProvider(email.toString())  // Fetch updated bookings
                            } else {
                                Toast.makeText(context, "Failed to accept booking", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return  // Exit after finding and processing the correct node
                    }
                }
                // If no matching booking was found
                Toast.makeText(context, "Booking not found", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
                Log.e("Firebase", "Error loading bookings", error.toException())
            }
        })
    }



    private fun cancelBooking(bookingKey: String) {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking_sprovider, null)

        // Create an AlertDialog
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true) // Allows the dialog to be canceled when tapping outside

        // Create the AlertDialog
        val alertDialog = builder.create()

        // Get references to the buttons and RadioGroup in the custom layout
        val btnYes: Button = dialogView.findViewById(R.id.btnYes)
        val btnNo: TextView = dialogView.findViewById(R.id.btnNo)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.optionsRadioGroup)

        // Set onClickListener for the Yes button
        btnYes.setOnClickListener {
            // Get the selected radio button ID
            val selectedOptionId = radioGroup.checkedRadioButtonId

            if (selectedOptionId != -1) { // Check if an option is selected
                // Find the selected radio button and get its text
                val selectedRadioButton: RadioButton = dialogView.findViewById(selectedOptionId)
                val cancellationReason = selectedRadioButton.text.toString()

                val databaseReference = FirebaseDatabase.getInstance().getReference("bookings/$bookingKey")

                // Add the cancellation reason and update the booking status
                val updates = mapOf(
                    "bookingCancelProvider" to cancellationReason,
                    "bookingStatus" to "Canceled"
                )

                databaseReference.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Booking canceled successfully", Toast.LENGTH_SHORT)
                            .show()
                        // Optionally, refresh the list of bookings after cancellation
                        email?.let { fetchBookingsForProvider(it) }
                    } else {
                        Toast.makeText(context, "Failed to cancel booking", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            alertDialog.dismiss() // Dismiss the dialog
        }

        // Set onClickListener for the No button
        btnNo.setOnClickListener {
            alertDialog.dismiss() // Just dismiss the dialog if no is clicked
        }

        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        alertDialog.show()
    }

    private fun fetchUserData(providerEmail: String, callback: (User) -> Unit) {
        val userReference = FirebaseDatabase.getInstance().getReference("users")

        userReference.orderByChild("email").equalTo(providerEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
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
