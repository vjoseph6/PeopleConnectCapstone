package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.SkillItem
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

// Adapter to display skills in RecyclerView
class SkillsAdapter(
    private var skillsList: List<SkillItem>,
    private val onSkillVisibilityChanged: (SkillItem) -> Unit,
    private val onSkillItemClick: (SkillItem) -> Unit // Add this callback for item click
) : RecyclerView.Adapter<SkillsAdapter.SkillsViewHolder>() {

    inner class SkillsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val skillImage: ShapeableImageView = itemView.findViewById(R.id.skillImage)
        val skillText: TextView = itemView.findViewById(R.id.skillText)
        val toggleButton: SwitchCompat = itemView.findViewById(R.id.my_switch)

        fun bind(skill: SkillItem) {
            skillText.text = skill.name
            Picasso.get().load(skill.image).into(skillImage)
            toggleButton.isChecked = skill.visible

            // Listener for the toggle button
            toggleButton.setOnCheckedChangeListener { _, isChecked ->
                skill.visible = isChecked
                onSkillVisibilityChanged(skill)
            }

            // New listener for the entire item
            itemView.setOnClickListener {
                onSkillItemClick(skill) // Call the item click callback
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sprovider_service_list, parent, false)
        return SkillsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkillsViewHolder, position: Int) {
        holder.bind(skillsList[position])
    }

    override fun getItemCount(): Int {
        return skillsList.size
    }

    // Method to update the skills list and notify adapter
    fun updateSkillsList(newSkillsList: List<SkillItem>) {
        this.skillsList = newSkillsList
        notifyDataSetChanged()
    }
}
