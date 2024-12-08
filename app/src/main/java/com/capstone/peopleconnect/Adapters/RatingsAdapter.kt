package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RatingsAdapter(
    private val ratings: List<Rating>,
    private val showServiceHeader: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_RATING = 1
    }

    inner class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProfile: ShapeableImageView = itemView.findViewById(R.id.imageProfile)
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textRating: TextView = itemView.findViewById(R.id.textRating)
        val textService: TextView = itemView.findViewById(R.id.textService)
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
    }

    inner class ServiceHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textServiceName: TextView = itemView.findViewById(R.id.textServiceName)
        val textAverageRating: TextView = itemView.findViewById(R.id.textAverageRating)
        val textRatingCount: TextView = itemView.findViewById(R.id.textRatingCount)
    }

    override fun getItemViewType(position: Int): Int {
        return if (showServiceHeader && shouldShowHeaderAtPosition(position)) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_RATING
        }
    }

    private fun shouldShowHeaderAtPosition(position: Int): Boolean {
        if (position == 0) return true
        val currentService = ratings[getAdjustedPosition(position)].serviceOffered
        val previousService = ratings[getAdjustedPosition(position - 1)].serviceOffered
        return currentService != previousService
    }

    private fun getAdjustedPosition(position: Int): Int {
        var headerCount = 0
        for (i in 0 until position) {
            if (shouldShowHeaderAtPosition(i)) headerCount++
        }
        return position - headerCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_service_rating_header, parent, false)
                ServiceHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_rating_provider_client_ratings, parent, false) // Updated layout name
                RatingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ServiceHeaderViewHolder -> {
                val service = ratings[getAdjustedPosition(position)].serviceOffered
                val serviceRatings = ratings.filter { it.serviceOffered == service }
                val averageRating = serviceRatings.map { it.rating }.average()

                holder.textServiceName.text = service
                holder.textAverageRating.text = String.format("%.1f ★", averageRating)
                holder.textRatingCount.text = "${serviceRatings.size} reviews"
            }
            is RatingViewHolder -> {
                val rating = ratings[getAdjustedPosition(position)]

                holder.textName.text = rating.name ?: "Unknown"
                holder.textRating.text = rating.rating.toString()
                holder.textService.text = rating.serviceOffered

                // Format and set date
                val date = rating.timestamp?.let {
                    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    sdf.format(Date(it))
                } ?: "Date not available"
                holder.textDate.text = date

                // Set category text based on rating value
                val categoryText = when (rating.rating) {
                    5.0f -> "Perfect"
                    4.0f -> "Great"
                    3.0f -> "Good"
                    2.0f -> "Fair"
                    1.0f -> "Poor"
                    else -> ""
                }
                holder.textCategory.text = categoryText

                // Load profile image
                if (!rating.profileImageUrl.isNullOrEmpty()) {
                    Picasso.get().load(rating.profileImageUrl)
                        .placeholder(R.drawable.profile1)
                        .into(holder.imageProfile)
                } else {
                    holder.imageProfile.setImageResource(R.drawable.profile1)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        var count = ratings.size
        if (showServiceHeader) {
            count += ratings.distinctBy { it.serviceOffered }.size
        }
        return count
    }
}