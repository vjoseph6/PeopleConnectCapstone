package com.capstone.peopleconnect.Adapters

import android.util.Log
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

class BookingSProviderAdapter(
    private var bookings: List<Pair<String, Bookings>>,  // Use Pair to hold the Firebase key and Booking
    private val fetchUserData: (String, (User) -> Unit) -> Unit,
    private val onAcceptBooking: (String) -> Unit,
    private val onCancelBooking: (String) -> Unit,
    private val onItemClickListener: (String) -> Unit // Add this parameter
) : RecyclerView.Adapter<BookingSProviderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnStatus: Button = itemView.findViewById(R.id.btnStatus)
        val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImage)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept_Present)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel_Present)
        val serviceTextView: TextView = itemView.findViewById(R.id.tvService)

        init {
            itemView.setOnClickListener {
                val bookingKey = bookings[adapterPosition].first
                onItemClickListener(bookingKey)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.present_item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (bookingKey, booking) = bookings[position]  // Get the booking and its Firebase key
        // Fetch user data based on provider email
        fetchUserData(booking.providerEmail) { user ->
            holder.nameTextView.text = user.name
            // Load the profile image using Picasso or Glide
            Picasso.get().load(user.profileImageUrl).into(holder.profileImageView)
        }

        // Check booking status and set visibility accordingly
        if (booking.bookingStatus != "Pending") {
            holder.btnAccept.visibility = View.GONE
            holder.btnCancel.visibility = View.GONE
            holder.btnStatus.visibility = View.VISIBLE
            holder.btnStatus.text = booking.bookingStatus
        } else {
            holder.btnAccept.visibility = View.VISIBLE
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnStatus.visibility = View.GONE
        }

        // Handle accept action
        holder.btnAccept.setOnClickListener {
            onAcceptBooking(bookingKey)
        }

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


        // Handle cancel action
        holder.btnCancel.setOnClickListener {
            onCancelBooking(bookingKey)
        }


        holder.serviceTextView.text = booking.serviceOffered
    }

    fun updateBookings(newBookings: List<Pair<String, Bookings>>) {
        this.bookings = newBookings
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = bookings.size
}