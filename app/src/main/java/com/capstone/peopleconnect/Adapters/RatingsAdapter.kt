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

        if (!rating.profileImageUrl.isNullOrEmpty()) {
            Picasso.get().load(rating.profileImageUrl).into(holder.imageProfile)
        } else {
            // Set a default image if the URL is empty
            holder.imageProfile.setImageResource(R.drawable.profile) // Replace with your default image resource
        }
    }
    override fun getItemCount(): Int {
        return ratings.size
    }
}
