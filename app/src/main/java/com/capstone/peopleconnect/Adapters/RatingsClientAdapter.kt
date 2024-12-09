package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RatingsClientAdapter(
    private var ratings: List<Rating>,
    private val showServiceCategory: Boolean = true
) : RecyclerView.Adapter<RatingsClientAdapter.RatingViewHolder>() {

    class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ShapeableImageView = itemView.findViewById(R.id.imageProfile)
        val nameTextView: TextView = itemView.findViewById(R.id.textName)
        val dateTextView: TextView = itemView.findViewById(R.id.textDate)
        val categoryTextView: TextView = itemView.findViewById(R.id.textCategory)
        val ratingTextView: TextView = itemView.findViewById(R.id.textRating)
        val serviceTextView: TextView = itemView.findViewById(R.id.textService)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rating_provider_client_ratings, parent, false)
        return RatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        val rating = ratings[position]

        // Load profile image
        if (rating.profileImageUrl?.isNotEmpty() == true) {
            Picasso.get().load(rating.profileImageUrl)
                .placeholder(R.drawable.profile1)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile1)
        }

        // Set name
        holder.nameTextView.text = rating.name

        holder.dateTextView.text = rating.raterEmail

        // Set rating
        holder.ratingTextView.text = String.format("%.1f", rating.rating)

        // Set service category
       /* if (showServiceCategory && rating.serviceOffered.isNotEmpty()) {
            holder.categoryTextView.text = rating.serviceOffered
            holder.categoryTextView.visibility = View.VISIBLE
        } else {
            holder.categoryTextView.visibility = View.GONE
        }*/

        // Set service text (if needed)
        holder.serviceTextView.text = rating.feedback
    }

    override fun getItemCount(): Int = ratings.size

    fun updateRatings(newRatings: List<Rating>) {
        ratings = newRatings
        notifyDataSetChanged()
    }
}