package com.capstone.peopleconnect.Message.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ChatUser(
    val userId: String,
    val name: String,
    val imageUrl: String
) : Parcelable

