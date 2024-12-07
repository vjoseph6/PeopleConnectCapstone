package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class ServiceProviderAdapter(
    private val serviceProviderList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<ServiceProviderAdapter.ServiceProviderViewHolder>() {

    class ServiceProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvImage: ShapeableImageView = itemView.findViewById(R.id.tvImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.provider_list, parent, false)
        return ServiceProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceProviderViewHolder, position: Int) {
        val user = serviceProviderList[position]

        holder.tvName.text = user.name

        // Check if profileImageUrl is not empty or null
        val imageUrl = user.profileImageUrl
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.profile1)  // Placeholder image while loading
                .error(R.drawable.profile1)        // Fallback error image if loading fails
                .into(holder.tvImage)
        } else {
            // If URL is empty or null, load the placeholder image
            Picasso.get()
                .load(R.drawable.profile1)
                .into(holder.tvImage)
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }


    override fun getItemCount(): Int {
        return serviceProviderList.size
    }
}
