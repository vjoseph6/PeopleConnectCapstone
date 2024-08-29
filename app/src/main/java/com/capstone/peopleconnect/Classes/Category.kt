package com.capstone.peopleconnect.Classes

data class Category(
    val image: String,
    val name: String,
    val interests: MutableList<Interest> // MutableList to reflect selections
)

