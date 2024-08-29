package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.Interest
import com.capstone.peopleconnect.R

class InterestAdapter(
    private val interests: MutableList<Interest>,
    private val onInterestSelected: (Interest) -> Unit
) : RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    private val selectedInterests = mutableSetOf<String>() // Track selected interests

    inner class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val interestNameTextView: TextView = itemView.findViewById(R.id.interestNameTextView)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        fun bind(interest: Interest) {
            interestNameTextView.text = interest.name

            // Temporarily remove the listener before updating the checkbox state
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = selectedInterests.contains(interest.name)

            // Reassign the listener to handle user interactions
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedInterests.add(interest.name)
                } else {
                    selectedInterests.remove(interest.name)
                }
                interest.isSelected = isChecked
                onInterestSelected(interest) // Notify the activity
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.interest_item, parent, false)
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val interest = interests[position]
        holder.bind(interest)
    }

    override fun getItemCount(): Int = interests.size

    fun updateData(newInterests: List<Interest>) {
        interests.clear()
        interests.addAll(newInterests)
        notifyDataSetChanged()
    }

    // Retrieve selected interests as a list of strings
    fun getSelectedInterests(): List<String> {
        return selectedInterests.toList()
    }
}
