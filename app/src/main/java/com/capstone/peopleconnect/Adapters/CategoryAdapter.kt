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

// Define a callback interface for item clicks
interface OnCategoryClickListener {
    fun onCategoryClick(category: Category)
}

class CategoryAdapter(
    private var categories: MutableList<Category>,
    private val listener: OnCategoryClickListener  // Listener to handle item clicks
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_fragment_list, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name
        Picasso.get()
            .load(category.image)
            .error(R.drawable.profile) // Optional error image
            .into(holder.tvImage)

        // Set a click listener for each category item
        holder.itemView.setOnClickListener {
            listener.onCategoryClick(category)
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
