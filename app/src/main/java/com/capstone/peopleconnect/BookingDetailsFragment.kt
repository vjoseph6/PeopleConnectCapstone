package com.capstone.peopleconnect

import SkillsPostsAdapter
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso


class BookingDetailsFragment : Fragment() {

    private var bookingId: String? = null

    // Views for displaying booking details
    private lateinit var providerEmailTextView: TextView
    private lateinit var bookingStatusTextView: TextView
    private lateinit var serviceOfferedTextView: TextView
    private lateinit var bookingStartTimeTextView: TextView
    private lateinit var bookingEndTimeTextView: TextView
    private lateinit var bookingDescriptionTextView: TextView
    private lateinit var bookingDayTextView: TextView
    private lateinit var bookingLocationTextView: TextView
    private lateinit var bookingAmountTextView: TextView
    private lateinit var paymentMethodTextView: TextView
    private lateinit var providerProfileImage: ShapeableImageView
    private lateinit var providerNameTextView: TextView
    private lateinit var clientProfileImage: ShapeableImageView
    private lateinit var clientNameTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString("BOOKING_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_details, container, false)

        // Initialize views
        bookingStatusTextView = view.findViewById(R.id.tvBookingStatus)
        serviceOfferedTextView = view.findViewById(R.id.tvServiceOffered)
        bookingStartTimeTextView = view.findViewById(R.id.tvBookingStartTime)
        bookingEndTimeTextView = view.findViewById(R.id.tvBookingEndTime)
        bookingDescriptionTextView = view.findViewById(R.id.tvBookingDescription)
        bookingDayTextView = view.findViewById(R.id.tvBookingDay)
        bookingLocationTextView = view.findViewById(R.id.tvBookingLocation)
        bookingAmountTextView = view.findViewById(R.id.tvBookingAmount)
        paymentMethodTextView = view.findViewById(R.id.tvPaymentMethod)
        providerProfileImage = view.findViewById(R.id.imgProviderProfile)
        providerNameTextView = view.findViewById(R.id.tvProviderName)
        clientProfileImage = view.findViewById(R.id.imgClientProfile)
        clientNameTextView = view.findViewById(R.id.tvClientName)
        imagesRecyclerView = view.findViewById(R.id.imageContainer)

        fetchBookingDetails()

        return view
    }

    private fun fetchBookingDetails() {
        val databaseReference =
            FirebaseDatabase.getInstance().getReference("bookings").child(bookingId ?: return)

        databaseReference.get().addOnSuccessListener { snapshot ->
            val booking = snapshot.getValue(Bookings::class.java)
            if (booking != null) {
                bookingStatusTextView.text = booking.bookingStatus
                serviceOfferedTextView.text = booking.serviceOffered
                bookingStartTimeTextView.text = booking.bookingStartTime
                bookingEndTimeTextView.text = booking.bookingEndTime
                bookingDescriptionTextView.text = booking.bookingDescription
                bookingDayTextView.text = booking.bookingDay
                bookingLocationTextView.text = booking.bookingLocation
                bookingAmountTextView.text = booking.bookingAmount.toString()
                paymentMethodTextView.text = booking.bookingPaymentMethod

                // Set up RecyclerView for displaying images
                setupImageRecyclerView(booking.bookingUploadImages)

                // Fetch Client and Service Provider details
                fetchClientDetails(booking.bookByEmail)
                fetchServiceProviderDetails(booking.providerEmail)
            }
        }.addOnFailureListener { exception ->
            Log.e("BookingDetailsFragment", "Error fetching booking details", exception)
        }
    }

    private fun setupImageRecyclerView(imageUrls: List<String>) {
        // Set up RecyclerView layout manager with a grid of 3 columns
        imagesRecyclerView.layoutManager = GridLayoutManager(context, 3)

        // Initialize and set the adapter for images
        val adapter = SkillsPostsAdapter(imageUrls) { imageUrl ->
            // Handle image click, show in full screen dialog
            val fullScreenDialog = Dialog(requireContext())
            fullScreenDialog.setContentView(R.layout.dialog_fullscreen_image)
            val fullScreenImageView = fullScreenDialog.findViewById<ImageView>(R.id.fullscreen_image)

            // Load the image into the full-screen view using Picasso
            Picasso.get().load(imageUrl).into(fullScreenImageView)

            // Show the full-screen dialog
            fullScreenDialog.show()
        }
        imagesRecyclerView.adapter = adapter
    }

    private fun fetchClientDetails(clientEmail: String) {
        val userDatabaseReference = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("email")
            .equalTo(clientEmail)

        userDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    clientNameTextView.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(clientProfileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingDetailsFragment", "Error fetching client details", error.toException())
            }
        })
    }

    private fun fetchServiceProviderDetails(providerEmail: String) {
        val userDatabaseReference = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("email")
            .equalTo(providerEmail)

        userDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    providerNameTextView.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(providerProfileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingDetailsFragment", "Error fetching service provider details", error.toException())
            }
        })
    }

    companion object {
        fun newInstance(bookingId: String) = BookingDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("BOOKING_ID", bookingId)
            }
        }
    }
}