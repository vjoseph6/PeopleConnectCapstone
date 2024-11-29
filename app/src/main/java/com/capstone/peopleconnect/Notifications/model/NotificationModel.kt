package com.capstone.peopleconnect.Notifications.model

data class NotificationModel(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // "chat", "call", or "booking"
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val channelId: String? = null,
    val callLink: String? = null,
    // Add these for booking notifications
    val bookingId: String? = null,
    val bookingStatus: String? = null,
    val bookingDate: String? = null,
    val bookingTime: String? = null,
    val cancellationReason: String? = null

)