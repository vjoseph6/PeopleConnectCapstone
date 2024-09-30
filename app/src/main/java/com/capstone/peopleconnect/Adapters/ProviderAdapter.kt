package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.ProviderData
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso

class ProviderAdapter(private val providerList: MutableList<ProviderData>) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    class ProviderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val providerName: TextView = view.findViewById(R.id.providerName)
        val providerCategory: TextView = view.findViewById(R.id.providerCategory) // Reference to provider category
        val providerDescription: TextView = view.findViewById(R.id.providerDescription)
        val providerImage: ImageView = view.findViewById(R.id.providerImage)
        val providerRating: RatingBar = view.findViewById(R.id.providerRating) // Reference to RatingBar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_recommended, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providerList[position]
        holder.providerName.text = provider.userName
        holder.providerCategory.text = provider.name // Displaying the provider category
        holder.providerDescription.text = provider.description
        holder.providerRating.rating = provider.rating ?: 0f // Safe call to handle null rating

        // Load provider image using Picasso
        if (!provider.imageUrl.isNullOrEmpty()) {
            Picasso.get().load(provider.imageUrl).into(holder.providerImage)
        } else {
            holder.providerImage.setImageResource(R.drawable.profile1) // Use a default image if no URL
        }
    }

    override fun getItemCount(): Int {
        return providerList.size
    }
}
