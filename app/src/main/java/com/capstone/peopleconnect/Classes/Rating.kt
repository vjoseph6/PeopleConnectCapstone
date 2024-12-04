package com.capstone.peopleconnect.Classes

data class Rating(
    val bookingId: String = "",
    val raterEmail: String = "",
    val ratedEmail: String = "",
    val rating: Float = 0f,
    val feedback: String = "",
    var name: String? = "",
    var profileImageUrl: String? = "",
    val timestamp: Long = 0,
)