package com.capstone.peopleconnect.Classes

data class Bookings(
    val bookByEmail: String = "",
    val providerEmail: String = "",
    val bookingStatus: String = "",
    val serviceOffered: String = "",
    val bookingStartTime: String = "",
    val bookingEndTime: String = "",
    val bookingDescription: String = "",
    val bookingDay: String = "",
    val bookingLocation: String = "",
    val bookingAmount: Double = 0.0,
    val bookingPaymentMethod: String = "",
    val bookingCancelClient: String = "",
    val bookingCancelProvider: String = "",
    val bookingUploadImages: List<String> = emptyList() // Store URLs of uploaded images
)
