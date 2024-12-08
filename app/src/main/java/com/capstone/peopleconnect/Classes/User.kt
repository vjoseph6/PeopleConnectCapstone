package com.capstone.peopleconnect.Classes

data class User(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val userId: String = "",
    var profileImageUrl: String = "",
    val roles: List<String> = listOf(),
    val userClicks: List<String> = listOf(),
    val userPref: List<String> = listOf(),
    val interest: List<String> = listOf(),
    val userRating: Float = 0.0f,
    val userNoOfBookings: Int = 0,
    val userTotalRating: Float = 0.0f,
    val status: String = "enabled"
)