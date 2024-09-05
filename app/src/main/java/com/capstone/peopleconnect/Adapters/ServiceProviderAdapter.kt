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

class ServiceProviderAdapter(private val serviceProviderList: List<User>) :
    RecyclerView.Adapter<ServiceProviderAdapter.ServiceProviderViewHolder>() {

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
        Picasso.get().load(user.profileImageUrl).into(holder.tvImage)
    }

    override fun getItemCount(): Int {
        return serviceProviderList.size
    }
}
