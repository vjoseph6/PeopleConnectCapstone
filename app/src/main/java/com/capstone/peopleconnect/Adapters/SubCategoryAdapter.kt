package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import android.widget.TextView
import com.capstone.peopleconnect.Classes.SubCategory

class SubCategoryAdapter(private var subCategories: MutableList<SubCategory>) :
    RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubCategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_fragment_list, parent, false)
        return SubCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubCategoryViewHolder, position: Int) {
        val subCategory = subCategories[position]
        holder.tvSubCategoryName.text = subCategory.name
        Picasso.get()
            .load(subCategory.image)
            .error(R.drawable.profile) // Optional error image
            .into(holder.ivSubCategoryImage)
    }

    override fun getItemCount(): Int {
        return subCategories.size
    }

    // ViewHolder class to hold the views for each subcategory item
    inner class SubCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubCategoryName: TextView = itemView.findViewById(R.id.tvName)
        val ivSubCategoryImage: ShapeableImageView = itemView.findViewById(R.id.tvImage)
    }

    // Method to update subcategories data
    fun updateSubCategories(newSubCategories: MutableList<SubCategory>) {
        subCategories = newSubCategories
        notifyDataSetChanged()
    }
}
