package com.capstone.peopleconnect.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookingSProviderAdapter(
    private var bookings: List<Pair<String, Bookings>>,
    private val fetchUserData: (String, (User) -> Unit) -> Unit,
    private val onAcceptBooking: (String) -> Unit,
    private val onCancelBooking: (String) -> Unit,
    private val onItemClickListener: (String) -> Unit,
    private val onItemLongClickListener: (String, Bookings) -> Unit
) : RecyclerView.Adapter<BookingSProviderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnStatus: Button = itemView.findViewById(R.id.btnStatus)
        val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImage)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept_Present)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel_Present)
        val serviceTextView: TextView = itemView.findViewById(R.id.tvService)

        init {
            // Existing click listener
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val bookingKey = bookings[position].first
                    onItemClickListener(bookingKey)
                }
            }

            // Update the long click listener with more detailed logging
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val (bookingKey, booking) = bookings[position]

                    // Add detailed debug logging
                    Log.d("BookingDebug", """
            Long Press Debug:
            - Booking ID: $bookingKey
            - Status: ${booking.bookingStatus}
            - Date: ${booking.bookingDay}
            - Start Time: ${booking.bookingStartTime}
            - Current Time: ${System.currentTimeMillis()}
            - Is Status Accepted: ${booking.bookingStatus == "Accepted"}
        """.trimIndent())

                    // Add logging inside isBookingTimeValid
                    val isTimeValid = isBookingTimeValid(booking)
                    Log.d("BookingDebug", "Is Time Valid: $isTimeValid")

                    when {
                        booking.bookingStatus != "Accepted" -> {
                            Log.d("BookingDebug", "Booking not accepted")
                            Toast.makeText(
                                itemView.context,
                                "This booking is ${booking.bookingStatus.toLowerCase()}. Only accepted bookings can be started.",
                                Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        !isTimeValid -> {
                            val bookingStartTime = booking.bookingStartTime.toLongOrNull() ?: return@setOnLongClickListener false
                            val hours = bookingStartTime.toInt() / 60
                            val minutes = bookingStartTime.toInt() % 60

                            Log.d("BookingDebug", "Time not valid yet. Hours: $hours, Minutes: $minutes")
                            Toast.makeText(
                                itemView.context,
                                "This booking will be available on ${booking.bookingDay} at ${String.format("%02d:%02d", hours, minutes)}. Please wait until the scheduled time.",
                                Toast.LENGTH_LONG
                            ).show()
                            true
                        }
                        else -> {
                            Log.d("BookingDebug", "Starting booking with key: $bookingKey")
                            onItemLongClickListener(bookingKey, booking)
                            true
                        }
                    }
                } else {
                    Log.e("BookingDebug", "Invalid position: $position")
                    false
                }
            }
        }

    }

    private fun isBookingTimeValid(booking: Bookings): Boolean {
        val currentTime = System.currentTimeMillis()
        val bookingStartTime = booking.bookingStartTime ?: return false
        val bookingDate = booking.bookingDay ?: return false

        try {
            // Parse date (yyyy-MM-dd format)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = dateFormatter.parse(bookingDate) ?: return false

            // Parse time (hh:mm a format, e.g., "10:30 PM")
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val parsedTime = timeFormatter.parse(bookingStartTime) ?: return false

            // Create calendar for booking time
            val bookingCalendar = Calendar.getInstance().apply {
                time = parsedDate
                val timeCalendar = Calendar.getInstance().apply { time = parsedTime }
                set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Create calendar for current time
            val currentCalendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
                // Add 8 hours to match GMT+8
                add(Calendar.HOUR_OF_DAY, 8)
            }

            // Debug logging
            Log.d("TimeValidation", """
            Time Validation Details:
            - Booking Date-Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bookingCalendar.time)}
            - Current Time (GMT+8): ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentCalendar.time)}
            - Raw Time Difference (minutes): ${(currentCalendar.timeInMillis - bookingCalendar.timeInMillis) / (60 * 1000)}
        """.trimIndent())

            // Compare the calendars
            return currentCalendar.timeInMillis >= bookingCalendar.timeInMillis

        } catch (e: Exception) {
            Log.e("TimeValidation", "Error parsing date/time: ${e.message}")
            e.printStackTrace()
            return false
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
        fetchUserData(booking.bookByEmail) { user ->
            holder.nameTextView.text = user.name
            // Load the profile image using Picasso or Glide
            Picasso.get().load(user.profileImageUrl).into(holder.profileImageView)
        }

        // Check booking status and set visibility accordingly
        if (booking.bookingStatus != "Pending") {
            holder.btnAccept.visibility = View.GONE
            holder.btnCancel.visibility = View.GONE
            holder.btnStatus.visibility = View.VISIBLE
            holder.btnStatus.text = if (booking.bookingStatus == "Complete") "Completed" else booking.bookingStatus
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
            "Accepted", "Accept" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.green)
            "Pending" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.red)
            "Complete", "Completed" -> holder.btnAccept.backgroundTintList =
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