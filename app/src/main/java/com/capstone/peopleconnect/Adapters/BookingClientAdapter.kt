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
import java.util.TimeZone

class BookingClientAdapter(
    private var bookings: List<Pair<String, Bookings>>,
    private val fetchUserData: (String, (User) -> Unit) -> Unit,
    private val onCancelBooking: (String) -> Unit,
    private val onItemClickListener: (String) -> Unit,
    private val onItemLongClickListener: (String, Bookings) -> Unit  // Add this new parameter
) : RecyclerView.Adapter<BookingClientAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImage)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept_Present)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel_Present)
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceOffered)
        val negotiateTextView: TextView = itemView.findViewById(R.id.tvNegotiation)

        init {
            // Existing click listener
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val bookingKey = bookings[position].first
                    onItemClickListener(bookingKey)
                }
            }

            // Update the long click listener with logging
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val (bookingKey, booking) = bookings[position]

                    // Log the attempt
                    Log.d("BookingAction", """
            Long Press Attempt:
            - Booking ID: $bookingKey
            - Status: ${booking.bookingStatus}
            - Date: ${booking.bookingDay}
            - Start Time: ${booking.bookingStartTime}
        """.trimIndent())

                    when {
                        booking.bookingStatus != "Accepted" -> {
                            Log.d("BookingAction", "Booking not accepted: ${booking.bookingStatus}")
                            Toast.makeText(
                                itemView.context,
                                "This booking is ${booking.bookingStatus.toLowerCase()}. Only accepted bookings can be started.",
                                Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        !isBookingTimeValid(booking) -> {
                            val bookingStartTime = booking.bookingStartTime.toLongOrNull() ?: return@setOnLongClickListener false
                            val hours = bookingStartTime.toInt() / 60
                            val minutes = bookingStartTime.toInt() % 60

                            Log.d("BookingAction", "Booking time not valid yet. Hours: $hours, Minutes: $minutes")
                            Toast.makeText(
                                itemView.context,
                                "This booking will be available on ${booking.bookingDay} at ${String.format("%02d:%02d", hours, minutes)}. Please wait until the scheduled time.",
                                Toast.LENGTH_LONG
                            ).show()
                            true
                        }
                        else -> {
                            Log.d("BookingAction", "Starting booking: $bookingKey")
                            onItemLongClickListener(bookingKey, booking)
                            true
                        }
                    }
                } else {
                    Log.e("BookingAction", "Invalid position: $position")
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
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")  // Set timezone to GMT+8
            }
            val parsedDate = dateFormatter.parse(bookingDate) ?: return false

            // Parse time (hh:mm a format, e.g., "10:30 PM")
            val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")  // Set timezone to GMT+8
            }
            val parsedTime = timeFormatter.parse(bookingStartTime) ?: return false

            // Create calendar for booking time
            val bookingCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                time = parsedDate
                val timeCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                    time = parsedTime
                }
                set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Create calendar for current time
            val currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                timeInMillis = currentTime
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Debug log for parsed values
            val debugFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")
            }

            Log.d("TimeValidation", """
            Booking Details:
            - Raw Date: $bookingDate
            - Raw Time: $bookingStartTime
            - Parsed Date-Time: ${debugFormatter.format(bookingCalendar.time)}
            - Current Time: ${debugFormatter.format(currentCalendar.time)}
            - Time Difference (minutes): ${(currentCalendar.timeInMillis - bookingCalendar.timeInMillis) / (60 * 1000)}
            - Is Valid: ${currentCalendar.timeInMillis >= bookingCalendar.timeInMillis}
        """.trimIndent())

            return currentCalendar.timeInMillis >= bookingCalendar.timeInMillis

        } catch (e: Exception) {
            Log.e("TimeValidation", "Error parsing date/time: ${e.message}")
            e.printStackTrace()
            return false
        }
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
        holder.btnAccept.text = when (booking.bookingStatus) {
            "Complete" -> "Completed"
            else -> booking.bookingStatus
        }
        when (booking.bookingStatus) {
            "Accepted" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.green)
            "Canceled" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.red)
            "Complete", "Completed" -> holder.btnAccept.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, R.color.green)
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




