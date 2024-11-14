package com.capstone.peopleconnect.Classes

data class Payments(
    val paymentId: String = "",
    val paymentMethod: String = "",
    val bookBy: String = "",
    val providerEmail: String = "",
    val paymentAmount: Double = 0.0,
    val paymentDate: String = "",
    val bookingId: String = "",
    val originalAmount: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val status: String = "completed" // You might want to track payment status
) 