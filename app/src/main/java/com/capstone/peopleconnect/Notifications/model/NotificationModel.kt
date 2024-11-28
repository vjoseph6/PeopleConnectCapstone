package com.capstone.peopleconnect.Notifications.model

data class NotificationModel(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // "chat" or "call"
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    // Add channelId for navigation purposes
    val channelId: String? = null,
    val callLink: String? = null  // Add this property
)