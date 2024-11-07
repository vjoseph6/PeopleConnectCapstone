package com.capstone.peopleconnect.Message.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.capstone.peopleconnect.Message.model.ChatUser
import com.capstone.peopleconnect.R

class UserAdapters(
    private val userList: List<ChatUser>,
    private val onUserClickListener: (ChatUser) -> Unit
) : RecyclerView.Adapter<UserAdapters.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sprovider_recent_message_items, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.displayName.text = user.name  // Using `user.name` instead of `user.firstName`

        Glide.with(holder.itemView.context)
            .load(user.imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.profile1)
            .error(R.drawable.profile1)
            .into(holder.profileImage)

        holder.itemView.setOnClickListener {
            onUserClickListener(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.sprovider_profile_pic)
        val displayName: TextView = itemView.findViewById(R.id.first_name_text) // Change to `name_text` if you've updated the layout ID
    }
}
