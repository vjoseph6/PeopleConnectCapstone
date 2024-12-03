package com.capstone.peopleconnect.Notifications.model

data class NotificationModel(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // "chat", "call", "booking", or "ongoing" -> "ongoing_arrive", "ongoing_working", "ongoing_complete_confirmation"
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    val channelId: String? = null,
    val callLink: String? = null,
    // Booking related fields
    val bookingId: String? = null,
    val bookingStatus: String? = null,
    val bookingDate: String? = null,
    val bookingTime: String? = null,
    val cancellationReason: String? = null,
    // Add progress state for ongoing notifications
    val progressState: String = "" // "arrive", "working", "awaiting_confirmation"
)