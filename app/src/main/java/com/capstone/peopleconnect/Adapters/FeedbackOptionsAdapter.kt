package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.R

class FeedbackOptionsAdapter(
    private val options: List<String>,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<FeedbackOptionsAdapter.ViewHolder>() {

    private val selectedOptions = mutableSetOf<String>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxFeedback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.checkBox.text = option
        holder.checkBox.isChecked = selectedOptions.contains(option)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedOptions.add(option)
            } else {
                selectedOptions.remove(option)
            }
            onSelectionChanged(selectedOptions.toList())
        }
    }

    override fun getItemCount() = options.size
}