package com.capstone.peopleconnect.Helper

import android.util.Log
import com.capstone.peopleconnect.Classes.BookingProgress
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference

object DatabaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val bookingProgressRef: DatabaseReference = database.getReference("booking_progress")

    fun updateBookingProgress(bookingId: String, progress: BookingProgress): Task<Void> {
        return if (!NetworkHelper.isNetworkAvailable.value!!) {
            throw IllegalStateException("No internet connection")
        } else if (isValidStateTransition(progress.state)) {
            bookingProgressRef.child(bookingId).setValue(progress)
                .addOnFailureListener { e ->
                    if (e is Exception) {
                        Log.e("DatabaseHelper", "Error updating progress", e)
                    }
                }
        } else {
            throw IllegalStateException("Invalid state transition to ${progress.state}")
        }
    }
    private fun isValidStateTransition(newState: String): Boolean {
        return when (newState) {
            BookingProgress.STATE_PENDING,
            BookingProgress.STATE_ARRIVE,
            BookingProgress.STATE_WORKING,
            BookingProgress.STATE_COMPLETE -> true
            else -> false
        }
    }

    fun getBookingProgressReference(bookingId: String): DatabaseReference {
        return bookingProgressRef.child(bookingId)
    }

    fun observeBookingProgress(bookingId: String, onProgressUpdate: (BookingProgress) -> Unit) {
        bookingProgressRef.child(bookingId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(BookingProgress::class.java)?.let { progress ->
                        onProgressUpdate(progress)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DatabaseHelper", "Error observing booking progress", error.toException())
                }
            })
    }


}