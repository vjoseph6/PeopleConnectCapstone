package com.capstone.peopleconnect.Classes

data class BookingProgress(
    val state: String = "PENDING", // PENDING, ARRIVE, WORKING, COMPLETE
    val timestamp: Long = System.currentTimeMillis(),
    val bookingId: String = "",
    val providerEmail: String = "",
    val clientEmail: String = ""
) {
    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_ARRIVE = "ARRIVE"
        const val STATE_WORKING = "WORKING"
        const val STATE_AWAITING_CONFIRMATION = "AWAITING_CLIENT_CONFIRMATION"
        const val STATE_COMPLETE = "COMPLETED"
    }

}
