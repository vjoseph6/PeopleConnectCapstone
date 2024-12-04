package com.capstone.peopleconnect.Helper

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object NotificationHelper {
    private var previousUnreadCount = 0

    fun setupNotificationBadge(
        fragment: Fragment,
        notificationBadge: TextView,
        tag: String
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(user.uid)

            notificationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unreadCount = 0
                    snapshot.children.forEach { notification ->
                        val isRead = notification.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) unreadCount++
                    }

                    fragment.activity?.runOnUiThread {
                        if (unreadCount > 0) {
                            notificationBadge.visibility = View.VISIBLE
                            notificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()

                            // Check if unread count has increased
                            if (unreadCount > previousUnreadCount) {
                                // Play notification sound and vibrate
                                fragment.context?.let { context ->
                                    playNotificationSound(context)
                                    vibrate(context)
                                }
                            }
                        } else {
                            notificationBadge.visibility = View.GONE
                        }
                        previousUnreadCount = unreadCount
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag, "Failed to read notifications", error.toException())
                }
            })
        }
    }

    private fun playNotificationSound(context: Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error playing sound", e)
        }
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error vibrating", e)
        }
    }

    // Add this new method for activity-level notification monitoring
    fun setupActivityNotificationMonitoring(
        context: Context,
        userId: String
    ) {
        val notificationsRef = FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(userId)

        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0
                snapshot.children.forEach { notification ->
                    val isRead = notification.child("isRead").getValue(Boolean::class.java) ?: false
                    if (!isRead) unreadCount++
                }

                // Only play sound/vibrate if unread count has increased
                if (unreadCount > previousUnreadCount) {
                    playNotificationSound(context)
                    vibrate(context)
                }
                previousUnreadCount = unreadCount
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationHelper", "Failed to read notifications", error.toException())
            }
        })
    }
}