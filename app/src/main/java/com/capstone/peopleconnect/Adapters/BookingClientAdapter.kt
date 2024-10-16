package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso

class BookingClientAdapter(
    private var bookings: List<Pair<String, Bookings>>,  // Now a Pair of key and booking
    private val fetchUserData: (String, (User) -> Unit) -> Unit,
    private val onCancelBooking: (String) -> Unit
) : RecyclerView.Adapter<BookingClientAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImage)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept_Present)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel_Present)
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceOffered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_booking_client_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (key, booking) = bookings[position]

        // Fetch and display user data using the provider's email
        fetchUserData(booking.providerEmail) { user ->
            holder.nameTextView.text = user.name
            Picasso.get().load(user.profileImageUrl).into(holder.profileImageView)
        }

        // Set the status of the booking
        holder.btnAccept.text = booking.bookingStatus
        when (booking.bookingStatus) {
            "Accepted" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.green)
            "Canceled" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.red)
            "Completed" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.blue)
            else -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.orange)
        }

        // Set visibility of the cancel button based on status
        if (booking.bookingStatus != "Pending") {
            holder.btnCancel.visibility = View.GONE
        } else {
            holder.btnCancel.visibility = View.VISIBLE
        }

        // Handle booking cancellation
        holder.btnCancel.setOnClickListener {
            onCancelBooking(key)  // Use the key (e.g., providerEmail or booking ID)
        }

        // Set the service offered in the booking
        holder.serviceTextView.text = booking.serviceOffered
    }

    override fun getItemCount(): Int = bookings.size

    // Method to update bookings for filtering
    fun updateBookings(newBookings: List<Pair<String, Bookings>>) {
        this.bookings = newBookings
        notifyDataSetChanged()
    }
}




