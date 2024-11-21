package com.capstone.peopleconnect.Classes

data class PostApplication(
    val postId: String = "",
    val providerEmail: String = "",
    val clientEmail: String = "",
    val status: String = "Pending"
) 