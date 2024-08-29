package com.capstone.peopleconnect.Classes

data class User(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val name: String = "",
    val email: String = "",
    val address: String = "",
    var profileImageUrl: String = "",
    val roles: List<String> = listOf(),
    val skills: List<String> = listOf(),
    val interest: List<String> = listOf()
)