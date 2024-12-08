package com.capstone.peopleconnect

import SkillsPostsAdapter
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import android.content.Intent
import android.net.Uri
import android.view.View.GONE
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient_ProviderProfile
import com.capstone.peopleconnect.Helper.StripeHelper
import com.capstone.peopleconnect.SPrvoider.Fragments.ActivityFragmentSProvider_ClientRatings


class BookingDetailsFragment : Fragment() {

    private var bookingId: String? = null
    private var isClient: Boolean = false
    private lateinit var stripeHelper: StripeHelper  // Add this line (Mao rani ako gi add/modify)

    private lateinit var providerEmailTextView: TextView
    private lateinit var bookingStatusTextView: TextView
    private lateinit var serviceOfferedTextView: TextView
    private lateinit var bookingStartTimeTextView: TextView
    private lateinit var bookingEndTimeTextView: TextView
    private lateinit var bookingDescriptionTextView: TextView
    private lateinit var bookingDayTextView: TextView
    private lateinit var resaonTextView: TextView
    private lateinit var bookingAmountTextView: TextView
    private lateinit var bookingTotalTextView: TextView
    private lateinit var paymentMethodTextView: TextView
    private lateinit var providerProfileImage: ShapeableImageView
    private lateinit var providerNameTextView: TextView
    private lateinit var clientProfileImage: ShapeableImageView
    private lateinit var clientNameTextView: TextView
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var btnBackClient: ImageButton
    private lateinit var clientRatingTextView: TextView
    private lateinit var providerRatingTextView: TextView
    private lateinit var layoutHide: RelativeLayout
    private  var bookByEmail: String? =  null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookingId = it.getString("BOOKING_ID")
            isClient = it.getBoolean("IS_CLIENT", false)
        }

        // Initialize StripeHelper here (Mao rani ako gi add/modify)
        stripeHelper = StripeHelper(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_details, container, false)
        btnBackClient = view.findViewById(R.id.btnBack)
        // Set background color based on isClient flag
        val layoutBackground = view.findViewById<ConstraintLayout>(R.id.layoutBackground)
        if (isClient) {
            layoutBackground.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))

            btnBackClient.setImageResource(R.drawable.backbtn2_client_)

            // Change background for all views with border_style
            val borderViews = listOf(
                view.findViewById<LinearLayout>(R.id.layout1),
                view.findViewById<LinearLayout>(R.id.layout2),
                view.findViewById<LinearLayout>(R.id.activitySummaryLayout),
                view.findViewById<TextView>(R.id.tvBookingDescription),
                view.findViewById<RecyclerView>(R.id.imageContainer),
                view.findViewById<RelativeLayout>(R.id.totalRateLayout)
            )

            borderViews.forEach { borderView ->
                borderView?.setBackgroundResource(R.drawable.border_style_2)
            }
        }

        // Initialize views
        bookingStatusTextView = view.findViewById(R.id.tvBookingStatus)
        serviceOfferedTextView = view.findViewById(R.id.tvServiceOffered)
        bookingStartTimeTextView = view.findViewById(R.id.tvBookingStartTime)
        bookingEndTimeTextView = view.findViewById(R.id.tvBookingEndTime)
        bookingDescriptionTextView = view.findViewById(R.id.tvBookingDescription)
        bookingDayTextView = view.findViewById(R.id.tvBookingDay)
        bookingAmountTextView = view.findViewById(R.id.tvBookingAmount)
        layoutHide = view.findViewById(R.id.layoutHide)
        bookingTotalTextView = view.findViewById(R.id.tvBookingTotals)
        paymentMethodTextView = view.findViewById(R.id.tvPaymentMethod)
        providerProfileImage = view.findViewById(R.id.imgProviderProfile)
        providerNameTextView = view.findViewById(R.id.tvProviderName)
        clientProfileImage = view.findViewById(R.id.imgClientProfile)
        clientNameTextView = view.findViewById(R.id.tvClientName)
        imagesRecyclerView = view.findViewById(R.id.imageContainer)
        clientRatingTextView = view.findViewById(R.id.clientRating)
        providerRatingTextView = view.findViewById(R.id.providerRating)
        resaonTextView = view.findViewById(R.id.tVCancellation)

        btnBackClient = view.findViewById(R.id.btnBack)
        btnBackClient.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val viewProfileTextView: TextView = view.findViewById(R.id.tvViewProfile)

        if (!isClient){
            viewProfileTextView.text = "Visit Client"
        }else{
            val viewLocationBtn: ImageButton = view.findViewById(R.id.viewLocationBtn)
            viewLocationBtn.visibility = GONE
            viewProfileTextView.text = "Visit Provider"}

        // Set click listener for the view profile TextView
       /* viewProfileTextView.setOnClickListener {
            if (!isClient) {
                // Navigate to ActivityFragmentSProvider_ClientRatings and pass the booking.bookByEmail
                val bookingEmail = bookByEmail
                val ratingsFragment = ActivityFragmentSProvider_ClientRatings.newInstance(bookingEmail.toString())
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, ratingsFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                // Navigate to ActivityFragmentClient_ProviderProfile and pass the provider's name
                val providerName = providerNameTextView.text.toString()
                val profileFragment = ActivityFragmentClient_ProviderProfile.newInstance(name = providerName, tag = "fromApplicants")
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, profileFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }*/

        // Initialize the location button and animate it
        val viewLocationBtn: ImageButton = view.findViewById(R.id.viewLocationBtn)

        // Animate the GIF using Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.location_gif)
            .into(viewLocationBtn)

        // Set up location button click listener
        viewLocationBtn.setOnClickListener {
            openLocationInGoogleMaps()
        }

        fetchBookingDetails()

        return view
    }

    // Modify the fetchBookingDetails function to use the class-level stripeHelper
    private fun fetchBookingDetails() {
        val databaseReference =
            FirebaseDatabase.getInstance().getReference("bookings").child(bookingId ?: return)

        databaseReference.get().addOnSuccessListener { snapshot ->
            val booking = snapshot.getValue(Bookings::class.java)
            if (booking != null) {
                bookingStatusTextView.text = booking.bookingStatus
                updateBookingStatusColor(booking.bookingStatus)
                serviceOfferedTextView.text = booking.serviceOffered
                bookingStartTimeTextView.text = booking.bookingStartTime
                bookingEndTimeTextView.text = booking.bookingEndTime
                bookingDescriptionTextView.text = "Description: ${booking.bookingDescription}"
                bookingDayTextView.text = booking.bookingDay
                bookingAmountTextView.text = booking.bookingScope
                bookingTotalTextView.text = "${booking.bookingAmount} PHP"
                paymentMethodTextView.text = booking.bookingPaymentMethod
                resaonTextView.text = booking.bookingCancelClient

                val viewProfileTextView: TextView? = view?.findViewById(R.id.tvViewProfile)

                viewProfileTextView?.setOnClickListener {
                    if (!isClient) {
                        // Navigate to ActivityFragmentSProvider_ClientRatings and pass the booking.bookByEmail
                        val bookingEmail = booking.bookByEmail.toString()
                        Log.d("CLIENT EMAIL", "$bookingEmail")
                        val ratingsFragment = ActivityFragmentSProvider_ClientRatings.newInstance(bookingEmail.toString())
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, ratingsFragment)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        // Navigate to ActivityFragmentClient_ProviderProfile and pass the provider's name
                        val providerName = providerNameTextView.text.toString()
                        val profileFragment = ActivityFragmentClient_ProviderProfile.newInstance(name = providerName, tag = "fromApplicants")
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, profileFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }


                if (booking.bookingScope == "Select Task Scope") {
                    // Ensure view is not null before accessing it
                    view?.findViewById<RelativeLayout>(R.id.layoutHide)?.visibility = View.GONE
                }

                if (booking.bookingCancelClient.isNullOrEmpty() ){
                    view?.findViewById<RelativeLayout>(R.id.layoutHide1)?.visibility = View.GONE
                }
                if (isClient) {
                    if (booking.bookingCancelProvider.isNullOrEmpty() ){
                        view?.findViewById<RelativeLayout>(R.id.layoutHide1)?.visibility = View.GONE
                    }
                    resaonTextView.text = booking.bookingCancelProvider
                }


                // Set up RecyclerView for displaying images
                setupImageRecyclerView(booking.bookingUploadImages)

                // Fetch Client and Service Provider details
                fetchClientDetails(booking.bookByEmail)
                bookByEmail = booking.bookByEmail
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
            val fullScreenImageView = fullScreenDialog.findViewById<ImageView>(R.id.fullscreenImageView)

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

        userDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    clientNameTextView.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(clientProfileImage)

                    // Set client rating
                    val rating = it.userRating ?: 0f
                    clientRatingTextView.text = "★ ${String.format("%.1f", rating)}"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingDetailsFragment", "Error fetching client details", error.toException())
            }
        })
    }

    private fun fetchServiceProviderDetails(providerEmail: String) {
        // Fetch provider user details
        val userDatabaseReference = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("email")
            .equalTo(providerEmail)

        userDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.children.firstOrNull()?.getValue(User::class.java)
                user?.let {
                    providerNameTextView.text = it.name
                    Picasso.get().load(it.profileImageUrl).into(providerProfileImage)

                    // After getting user details, fetch their skill rating
                    fetchProviderSkillRating(providerEmail)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingDetailsFragment", "Error fetching service provider details", error.toException())
            }
        })
    }

    private fun fetchProviderSkillRating(providerEmail: String) {
        val skillsRef = FirebaseDatabase.getInstance().getReference("skills")

        skillsRef.orderByChild("user").equalTo(providerEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (skillSetSnapshot in snapshot.children) {
                            val skillItems = skillSetSnapshot.child("skillItems")
                            for (skillItem in skillItems.children) {
                                val skillName = skillItem.child("name").getValue(String::class.java)
                                if (skillName == serviceOfferedTextView.text.toString()) {
                                    val rating = skillItem.child("rating").getValue(Float::class.java) ?: 0f
                                    providerRatingTextView.text = "★ ${String.format("%.1f", rating)}"
                                    return
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BookingDetailsFragment", "Error fetching provider skill rating", error.toException())
                }
            })
    }

    private fun updateBookingStatusColor(status: String) {
        val color = when (status) {
            "Pending" -> R.color.orange
            "Accepted" -> R.color.green
            "Canceled" -> R.color.red
            "Complete", "Completed" -> if (isClient) R.color.green else R.color.blue
            else -> R.color.black
        }
        bookingStatusTextView.setTextColor(ContextCompat.getColor(requireContext(), color))
        
        // Add this to convert "Complete" to "Completed" in display
        bookingStatusTextView.text = if (status == "Complete") "Completed" else status
    }

    private fun openLocationInGoogleMaps() {
        val databaseReference = FirebaseDatabase.getInstance()
            .getReference("bookings")
            .child(bookingId ?: return)

        databaseReference.get().addOnSuccessListener { snapshot ->
            val booking = snapshot.getValue(Bookings::class.java)
            booking?.let {
                val location = it.bookingLocation
                if (!location.isNullOrEmpty()) {
                    // Create a Uri for Google Maps search
                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")

                    // Create an Intent to open Google Maps
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")

                    // Verify that Google Maps is installed
                    if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        // If Google Maps is not installed, open in browser
                        val browserIntent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(location)}")
                        )
                        startActivity(browserIntent)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("BookingDetailsFragment", "Error fetching location", exception)
        }
    }

    companion object {
        fun newInstance(bookingId: String, isClient: Boolean = false) = BookingDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("BOOKING_ID", bookingId)
                putBoolean("IS_CLIENT", isClient)
            }
        }
    }
}