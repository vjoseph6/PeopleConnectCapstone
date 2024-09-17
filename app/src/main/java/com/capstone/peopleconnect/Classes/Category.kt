package com.capstone.peopleconnect.Classes

data class Category(
    var image: String = "",
    var name: String = "",
    var interests: MutableList<Interest> = mutableListOf() // MutableList to reflect selections
)

