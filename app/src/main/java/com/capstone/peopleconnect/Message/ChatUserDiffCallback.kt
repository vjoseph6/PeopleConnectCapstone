package com.capstone.peopleconnect.Message

import androidx.recyclerview.widget.DiffUtil
import com.capstone.peopleconnect.Message.model.ChatUser

class ChatUserDiffCallback(
    private val oldList: List<ChatUser>,
    private val newList: List<ChatUser>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].userId == newList[newItemPosition].userId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
