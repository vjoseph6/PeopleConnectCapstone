package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import android.widget.TextView

class CategoryAdapter(
    private var categories: MutableList<Category>,
    private val listener: (Category) -> Unit  // Listener to handle category clicks
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_fragment_list, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name

        // Add null/empty check for image URL
        if (!category.image.isNullOrEmpty()) {
            Picasso.get()
                .load(category.image)
                .error(R.drawable.profile)
                .into(holder.tvImage)
        } else {
            // Load default image if URL is null or empty
            holder.tvImage.setImageResource(R.drawable.profile)
        }

        // Set a click listener for each category item
        holder.itemView.setOnClickListener {
            listener(category)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    // ViewHolder class to hold the views for each item
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvImage: ShapeableImageView = itemView.findViewById(R.id.tvImage)
    }

    // Method to update categories data
    fun updateCategories(newCategories: MutableList<Category>) {
        categories = newCategories
        notifyDataSetChanged()  // Notify the adapter that the data has changed
    }
}

