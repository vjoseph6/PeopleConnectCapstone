package com.capstone.peopleconnect.Notifications.adapter

class NotificationAdapter {
}

//  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

//package com.capstone.peopleconnect.Notifications.adapter
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.cardview.widget.CardView
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.RecyclerView
//import com.capstone.peopleconnect.Notifications.model.NotificationModel
//
//import com.capstone.peopleconnect.R
//
//class NotificationAdapter(
//    private val context: Context,
//    private val onNotificationClick: (NotificationModel) -> Unit
//) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
//
//    private var notifications = mutableListOf<NotificationModel>()
//
//    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val iconView: ImageView = itemView.findViewById(R.id.notification_icon)
//        private val titleView: TextView = itemView.findViewById(R.id.notification_title)
//        private val descriptionView: TextView = itemView.findViewById(R.id.notification_description)
//        private val cardView: CardView = itemView.findViewById(R.id.card_view_item)
//
//        fun bind(notification: NotificationModel) {
//            titleView.text = notification.title
//            descriptionView.text = notification.description
//
//            // Set icon based on notification type
//            val iconResource = when (notification.type) {
//                "chat" -> R.drawable.ic_notification // Make sure you have this drawable
//                "call" -> R.drawable.ic_video_call // Make sure you have this drawable
//                else -> R.drawable.client_activity
//            }
//            iconView.setImageResource(iconResource)
//
//            // Set background color based on read status
//            cardView.setCardBackgroundColor(
//                if (notification.isRead)
//                    ContextCompat.getColor(context, R.color.white)
//                else
//                    ContextCompat.getColor(context, R.color.light_gray) // Add this color to your resources
//            )
//
//            // Handle click
//            itemView.setOnClickListener {
//                onNotificationClick(notification)
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.sprovider_notification_items, parent, false)
//        return NotificationViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
//        holder.bind(notifications[position])
//    }
//
//    override fun getItemCount() = notifications.size
//
//    fun updateNotifications(newNotifications: List<NotificationModel>) {
//        notifications.clear()
//        notifications.addAll(newNotifications)
//        notifyDataSetChanged()
//    }
//}