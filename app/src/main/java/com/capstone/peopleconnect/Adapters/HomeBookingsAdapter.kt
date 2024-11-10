import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Bookings
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeBookingsAdapter(
    private val bookingList: List<Bookings>,
    private val database: FirebaseDatabase
) : RecyclerView.Adapter<HomeBookingsAdapter.BookingViewHolder>() {

    private var profileImageUrl: String? = null
    private var userName: String? = null

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ShapeableImageView = itemView.findViewById(R.id.ivProfileImage)
        val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        val locationTextView: TextView = itemView.findViewById(R.id.tvLocation)
        val dayTextView: TextView = itemView.findViewById(R.id.tvDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sprovider_taskoverview_item, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookingList[position]
        holder.nameTextView.text = userName ?: booking.bookByEmail
        holder.locationTextView.text = booking.bookingLocation

        // Format the booking day for display
        holder.dayTextView.text = formatBookingDay(booking.bookingDay)

        // Set profile image if available
        profileImageUrl?.let {
            Picasso.get()
                .load(it)
                .error(R.drawable.profile)
                .into(holder.profileImageView)
        } ?: holder.profileImageView.setImageResource(R.drawable.profile)
    }

    override fun getItemCount(): Int = bookingList.size

    fun setProfileImageUrl(imageUrl: String?) {
        this.profileImageUrl = imageUrl
        notifyDataSetChanged()  // Refresh the adapter to update the profile image
    }

    fun setUserName(name: String?) {
        this.userName = name
        notifyDataSetChanged()  // Refresh the adapter to update the name
    }

    private fun formatBookingDay(bookingDate: String?): String {
        if (bookingDate.isNullOrEmpty()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = inputFormat.parse(bookingDate) ?: return bookingDate

        val calendar = Calendar.getInstance()
        calendar.time = date

        val currentCalendar = Calendar.getInstance()
        val weekStart = currentCalendar.apply { set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) }.time
        val weekEnd = currentCalendar.apply { set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY) }.time

        return if (date.after(weekStart) && date.before(weekEnd)) {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        } else {
            outputFormat.format(date)
        }
    }
}


