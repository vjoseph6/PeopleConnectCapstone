package com.capstone.peopleconnect.Classes

data class Interest(
    val name: String,          // Name to display in the TextView
    val type: String,          // Type of the interest
    var isSelected: Boolean = false // Selection state
)