package com.capstone.peopleconnect.Classes

data class Bookings(
    val bookingId: String = "",
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
    val bookingCommissionAmount: Double = 0.0,
    val bookingTotalAmount: Double = 0.0,
    val bookingPaymentMethod: String = "",
    val bookingPaymentId: String = "",
    val bookingCancelClient: String = "",
    val bookingCancelProvider: String = "",
    val bookingUploadImages: List<String> = listOf(),
    val receiptSent: Boolean = false,
    val receiptSentDate: String = "",
    val bookingScope: String = ""
)
