package com.capstone.peopleconnect.Classes

data class PostApplication(
    val postId: String = "",
    val providerEmail: String = "",
    val clientEmail: String = "",
    var status: String = "Pending"
) 