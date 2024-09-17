package com.capstone.peopleconnect.Classes

data class Interest(
    val name: String = "",
    val image: String = "", // Add this if you need to handle image URLs
    var isSelected: Boolean = false // Selection state
)