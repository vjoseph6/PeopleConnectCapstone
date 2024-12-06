package com.capstone.peopleconnect.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.ProviderData
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient_ReviewAndConfirm
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso

class ProviderAdapter(
    private val providerList: MutableList<ProviderData>,
    private val onItemClicked: (ProviderData) -> Unit // Pass a lambda for item clicks
) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    class ProviderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val providerName: TextView = view.findViewById(R.id.providerName)
        val providerCategory: TextView = view.findViewById(R.id.providerCategory)
        val providerDescription: TextView = view.findViewById(R.id.providerDescription)
        val providerImage: ImageView = view.findViewById(R.id.providerImage)
        val skillRate: TextView = view.findViewById(R.id.providerPrice)
        val providerRating: RatingBar = view.findViewById(R.id.providerRating)
        val providerLocation: TextView = view.findViewById(R.id.providerLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_recommended, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providerList[position]
        holder.providerName.text = provider.userName
        holder.providerCategory.text = provider.name
        holder.providerDescription.text = provider.description
        holder.skillRate.text = "₱ ${provider.skillRate} /hr"
        holder.providerRating.rating = provider.rating ?: 0f
        holder.providerLocation.text = simplifyLocation(provider.location)

        // Load provider image using Picasso
        if (!provider.imageUrl.isNullOrEmpty()) {
            Picasso.get().load(provider.imageUrl).into(holder.providerImage)
        } else {
            holder.providerImage.setImageResource(R.drawable.profile1)
        }

        // Set item click listener
        holder.itemView.setOnClickListener {
            onItemClicked(provider) // Pass the provider data to the listener
        }
    }

    fun simplifyLocation(fullLocation: String?): String {
        // Check if the location is valid
        if (fullLocation.isNullOrEmpty()) return "No location available"

        // Split the location by commas
        val parts = fullLocation.split(",").map { it.trim() }

        // Check if the string contains enough parts to extract city and province
        return if (parts.size >= 3) {
            "${parts[parts.size - 3]}, ${parts[parts.size - 2]}" // Extract City and Province
        } else {
            fullLocation // Return original if parts are insufficient
        }
    }


    override fun getItemCount(): Int {
        return providerList.size
    }

    fun updateList(newList: List<ProviderData>) {
        providerList.clear()
        providerList.addAll(newList)
        notifyDataSetChanged()
    }

}

