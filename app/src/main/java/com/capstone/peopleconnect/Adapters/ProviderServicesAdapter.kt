package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class ProviderServicesAdapter(
    private val services: MutableList<Category> = mutableListOf(),
    private var selectedService: String = "",
    private val onServiceSelected: (String) -> Unit
) : RecyclerView.Adapter<ProviderServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: RelativeLayout = itemView.findViewById(R.id.itemContainer)
        private val imageView: ShapeableImageView = itemView.findViewById(R.id.tvImage)
        private val nameText: TextView = itemView.findViewById(R.id.tvName)

        fun bind(service: Category) {
            nameText.text = service.name
            
            // Load service image
            if (!service.image.isNullOrEmpty()) {
                Picasso.get()
                    .load(service.image)
                    .error(R.drawable.profile)
                    .into(imageView)
            }

            // Handle selection state
            val isSelected = service.name == selectedService
            container.isSelected = isSelected
            nameText.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isSelected) R.color.white else R.color.black
                )
            )

            // Handle click
            itemView.setOnClickListener {
                if (service.name != selectedService) {
                    val oldSelectedService = selectedService
                    selectedService = service.name
                    onServiceSelected(service.name)
                    
                    // Update only the affected items
                    val oldPosition = services.indexOfFirst { it.name == oldSelectedService }
                    val newPosition = services.indexOfFirst { it.name == service.name }
                    
                    if (oldPosition != -1) notifyItemChanged(oldPosition)
                    if (newPosition != -1) notifyItemChanged(newPosition)
                    
                    Toast.makeText(
                        itemView.context,
                        "Service changed to ${service.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount() = services.size

    fun updateServices(newServices: List<Category>) {
        services.clear()
        services.addAll(newServices)
        notifyDataSetChanged()
    }

    fun updateSelectedService(newSelectedService: String) {
        val oldSelectedService = selectedService
        selectedService = newSelectedService
        
        // Update only the affected items
        val oldPosition = services.indexOfFirst { it.name == oldSelectedService }
        val newPosition = services.indexOfFirst { it.name == newSelectedService }
        
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }
} 