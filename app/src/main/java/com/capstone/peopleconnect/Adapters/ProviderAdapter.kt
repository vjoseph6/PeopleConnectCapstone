package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.ProviderData
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView

import com.squareup.picasso.Picasso

class ProviderAdapter(private val providerList: List<ProviderData>) :
    RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_recommended, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val providerData = providerList[position]

        holder.providerNameTextView.text = providerData.name
        holder.skillRateTextView.text = if (providerData.skillRate != null) {
            "₱${providerData.skillRate}/hr"
        } else {
            "N/A"
        }

        holder.descriptionTextView.text = providerData.description
        holder.userNameTextView.text = providerData.userName ?: "Unknown"

        // Load image using Picasso
        Picasso.get()
            .load(providerData.imageUrl)
            .placeholder(R.drawable.profile) // Default profile image
            .error(R.drawable.profile) // Error image if loading fails
            .into(holder.providerImageView)
    }

    override fun getItemCount(): Int = providerList.size

    class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val providerNameTextView: TextView = itemView.findViewById(R.id.providerCategory)
        val skillRateTextView: TextView = itemView.findViewById(R.id.providerPrice)
        val descriptionTextView: TextView = itemView.findViewById(R.id.providerDescription)
        val userNameTextView: TextView = itemView.findViewById(R.id.providerName)
        val providerImageView: ShapeableImageView = itemView.findViewById(R.id.providerImage) // ImageView for the profile image
    }
}
