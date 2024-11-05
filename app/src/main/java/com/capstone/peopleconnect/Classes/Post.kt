package com.capstone.peopleconnect.Classes

data class Post(
    val postId: String,
    val postDescription: String,
    val email: String?,
    val categoryName: String?,
    val postImages: List<String>,
    val postStatus: String?
)
