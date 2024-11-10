package com.capstone.peopleconnect.SPrvoider.Fragments

import HomeBookingsAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class HomeFragmentSProvider : Fragment() {

    private var email: String? = null
    private var nameTextView: TextView? = null
    private lateinit var rvInterests: RecyclerView
    private val bookingList = mutableListOf<Bookings>()

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
        return inflater.inflate(R.layout.fragment_home_s_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDateText(view)

         nameTextView = view.findViewById(R.id.tvName)


        // Initialize RecyclerView
        rvInterests = view.findViewById(R.id.rvInterests)
        rvInterests.layoutManager = LinearLayoutManager(requireContext())
        val bookingAdapter = HomeBookingsAdapter(bookingList, FirebaseDatabase.getInstance())
        rvInterests.adapter = bookingAdapter

        val currentEmail = email ?: return
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")

        // Fetch bookings for the current provider
        bookingsRef.orderByChild("providerEmail").equalTo(currentEmail)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bookingList.clear()
                    for (bookingSnapshot in snapshot.children) {
                        val booking = bookingSnapshot.getValue(Bookings::class.java)
                        booking?.let { bookingList.add(it) }
                    }
                    bookingAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load bookings", Toast.LENGTH_SHORT).show()
                }
            })

        fetchUserData(currentEmail) { userName, profileImageUrl ->
            bookingAdapter.setProfileImageUrl(profileImageUrl)  // Set profile image URL in adapter
            bookingAdapter.setUserName(userName)                // Set user name in adapter
        }


        // Set up icons and click listeners
        setupIconClickListeners(view)
    }


    private fun fetchUserData(providerEmail: String, callback: (String?, String?) -> Unit) {
        val userReference = FirebaseDatabase.getInstance().getReference("users")
        userReference.orderByChild("email").equalTo(providerEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        val userName = userSnapshot.child("name").value as? String
                        val profileImageUrl = userSnapshot.child("profileImageUrl").value as? String
                        nameTextView?.text = userSnapshot.child("firstName").value as? String
                        callback(userName, profileImageUrl) // Pass name and profile image URL
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }



    private fun updateDateText(view: View) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val currentDate = dateFormat.format(Date())
        view.findViewById<TextView>(R.id.tvDate_SPROVIDER).text = currentDate
    }

    private fun setupIconClickListeners(view: View) {
        val ivFilter: ImageView = view.findViewById(R.id.ivFilter)
        ivFilter.setOnClickListener { showFilterDialog() }

        view.findViewById<LinearLayout>(R.id.notificationLayout_sprovider).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, NotificationFragmentSProvider())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<LinearLayout>(R.id.messageLayout_sprovider).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, MessageFragmentSProvider())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.sprovider_dialog_filter_options, null)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(dialogView)

        // Handle button clicks with a delay
        listOf(R.id.btnToday, R.id.btnTomorrow, R.id.btnUpcoming).forEach { buttonId ->
            dialogView.findViewById<Button>(buttonId).setOnClickListener {
                it.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
                it.postDelayed({ bottomSheetDialog.dismiss() }, 200)
            }
        }

        bottomSheetDialog.show()
    }
    companion object {

        @JvmStatic
        fun newInstance(email: String) =
            HomeFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                }
            }
    }
}