package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class RatingsAdapter(private val ratings: List<Rating>) : RecyclerView.Adapter<RatingsAdapter.RatingViewHolder>() {

    inner class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProfile: ImageView = itemView.findViewById(R.id.imageProfile)
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        val textRating: TextView = itemView.findViewById(R.id.textRating)
        val textService: TextView = itemView.findViewById(R.id.textService) // Add this line
        val textCategory: TextView = itemView.findViewById(R.id.textCategory) // Add this line
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rating_client_provider_ratings, parent, false)
        return RatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        val rating = ratings[position]
        holder.textName.text = rating.name ?: "Unknown"
        holder.textDescription.text = rating.feedback
        holder.textRating.text = rating.rating.toString()
        holder.textService.text = rating.serviceOffered // Add this line

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

        if (!rating.profileImageUrl.isNullOrEmpty()) {
            Picasso.get().load(rating.profileImageUrl).into(holder.imageProfile)
        } else {
            holder.imageProfile.setImageResource(R.drawable.profile)
        }
    }

    override fun getItemCount(): Int {
        return ratings.size
    }
}
