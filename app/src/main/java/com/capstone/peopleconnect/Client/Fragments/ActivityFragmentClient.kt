package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ActivityFragmentClient : Fragment() {

    private lateinit var emptyView: RelativeLayout
    private lateinit var adapter: BookingClientAdapter
    private lateinit var recyclerView: RecyclerView
    private val bookings = mutableListOf<Pair<String, Bookings>>()  // Store Pair of key and booking
    private var email: String? = null
    private val allBookings = mutableListOf<Pair<String, Bookings>>()// Store all bookings for filtering
    private var currentFilter: String = "Booking"  // Track the current tab/filter

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
        adapter = BookingClientAdapter(bookings, ::fetchUserData, ::cancelBooking)
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

        return view
    }

    // Filters bookings based on status
    private fun filterBookings(statusFilter: String) {
        val filteredBookings = when (statusFilter) {
            "Booking" -> allBookings.filter { it.second.bookingStatus != "Canceled" && it.second.bookingStatus != "Completed" }
            "Successful" -> allBookings.filter { it.second.bookingStatus == "Completed" }
            "Failed" -> allBookings.filter { it.second.bookingStatus == "Canceled" }
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
    private fun highlightSelectedTab(selectedTab: TextView, vararg otherTabs: TextView) {
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        otherTabs.forEach { it.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)) }
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
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_booking, null)

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

                // Proceed with the cancellation
                val databaseReference = FirebaseDatabase.getInstance().getReference("bookings/$bookingKey")

                // Add the cancellation reason to the booking
                val updates = mapOf(
                    "bookingCancelClient" to cancellationReason
                )

                // Update Firebase with the cancellation reason and remove the booking
                databaseReference.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Proceed to remove the booking
                        databaseReference.removeValue().addOnCompleteListener { removeTask ->
                            if (removeTask.isSuccessful) {
                                Toast.makeText(context, "Booking canceled successfully", Toast.LENGTH_SHORT).show()
                                // Optionally, refresh the list of bookings after cancellation
                                email?.let { fetchBookingsForClient(it) }
                                alertDialog.dismiss()
                            } else {
                                Toast.makeText(context, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to update cancellation reason", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Show a message if no option is selected
                Toast.makeText(context, "Please select a reason for canceling", Toast.LENGTH_SHORT).show()
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

