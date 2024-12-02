package com.capstone.peopleconnect.Classes

data class Post(
    val postId: String = "",
    val postDescription: String = "",
    val email: String? = "",
    val categoryName: String = "",
    val postImages: List<String> = listOf(),
    val postStatus: String = "",
    val client: Boolean = false,
    val status: String = "Open",
    val bookDay: String = "",
    val startTime: String = "",
    var startDate: String? = "",
    val endTime: String = ""
)
