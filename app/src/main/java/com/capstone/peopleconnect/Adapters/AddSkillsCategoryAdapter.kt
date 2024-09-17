package com.capstone.peopleconnect.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Category
import com.capstone.peopleconnect.R
import android.widget.ImageView
import android.widget.TextView

class AddSkillsCategoryAdapter(
    private var categories: MutableList<Category>,
    private val listener: (Category) -> Unit  // Modify the type to Category
) : RecyclerView.Adapter<AddSkillsCategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skills_category_list, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name

        // Handle click event directly using the listener
        holder.itemView.setOnClickListener {
            listener(category)  // Pass the category object to the listener
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.textView)
        val ivArrow: ImageView = itemView.findViewById(R.id.arrowImg)
    }

    fun updateCategories(newCategories: MutableList<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
