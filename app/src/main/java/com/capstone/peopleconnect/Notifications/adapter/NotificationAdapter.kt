// PART OF NOTIFICATION

package com.capstone.peopleconnect.Notifications.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Notifications.model.NotificationModel

import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(
    private val context: Context,
    private val onNotificationClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private var notifications = mutableListOf<NotificationModel>()

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.notification_icon)
        private val titleView: TextView = itemView.findViewById(R.id.notification_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.notification_description)
        private val cardView: CardView = itemView.findViewById(R.id.card_view_item)

        fun bind(notification: NotificationModel) {
            titleView.text = notification.title
            descriptionView.text = notification.description

            // Set icon based on notification type
            val iconResource = when (notification.type) {
                "chat" -> R.drawable.ic_notification
                "call" -> R.drawable.ic_video_call
                else -> R.drawable.client_activity
            }
            iconView.setImageResource(iconResource)

            // Update background color based on read status from Firebase
            updateBackgroundColor(notification.isRead)

            // Handle click
            itemView.setOnClickListener {
                // Update Firebase and local state
                notification.id.let { notificationId ->
                    val notificationsRef = FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        .child(notificationId)

                    notificationsRef.child("isRead").setValue(true)
                        .addOnSuccessListener {
                            // Update local state
                            notification.isRead = true
                            // Update UI
                            updateBackgroundColor(true)
                        }
                }
                onNotificationClick(notification)
            }
        }

        private fun updateBackgroundColor(isRead: Boolean) {
            cardView.setCardBackgroundColor(
                if (isRead)
                    ContextCompat.getColor(context, R.color.white)
                else
                    ContextCompat.getColor(context, R.color.light_gray)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sprovider_notification_items, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<NotificationModel>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}