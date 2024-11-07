package com.capstone.peopleconnect.Client.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Message.ChatUserDiffCallback
import com.capstone.peopleconnect.Message.adapter.UserAdapters
import com.capstone.peopleconnect.Message.chat.ChatActivity
import com.capstone.peopleconnect.Message.model.ChatUser
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class MessageFragmentClient : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapters
    private var chatUser = mutableListOf<ChatUser>()

    private var currentUser: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var loaderMessage: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()  // Initialize FirebaseAuth here
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_message_client, container, false)

        loaderMessage = rootView.findViewById(R.id.LoadUserMessages)
        recyclerView = rootView.findViewById(R.id.message_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        chatUser = ArrayList()

        userAdapter = UserAdapters(chatUser) { user ->
            openChatWithUser(user)
        }
        recyclerView.adapter = userAdapter

        fetchUsersFromFirebase()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the back button and set an OnClickListener
        val backButton: ImageView = view.findViewById(R.id.back_button_message)
        backButton.setOnClickListener {
            // Navigate back to the HomeFragmentClient
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun fetchUsersFromFirebase() {
        val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
        loaderMessage.visibility = View.VISIBLE

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newChatUserList = mutableListOf<ChatUser>()
                for (userSnapshot in snapshot.children) {
                    // Retrieve data with default values
                    val userId = userSnapshot.child("userId").getValue(String::class.java) ?: ""
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                    val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                    val roles = userSnapshot.child("roles").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf()

                    // Only add users who have the role "Service Provider" and are not the current user
                    if ("Service Provider" in roles && userId != getCurrentUserId()) {
                        newChatUserList.add(ChatUser(userId, name, profileImageUrl))
                    }
                }

                // Efficiently update the RecyclerView using DiffUtil
                val diffCallback = ChatUserDiffCallback(chatUser, newChatUserList)
                val diffResult = DiffUtil.calculateDiff(diffCallback)

                chatUser.clear()
                chatUser.addAll(newChatUserList)
                diffResult.dispatchUpdatesTo(userAdapter)

                loaderMessage.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("UserMessage", "Error getting data.", error.toException())
                Toast.makeText(context, "Failed to load users. Please try again.", Toast.LENGTH_SHORT).show()
                loaderMessage.visibility = View.GONE
            }
        })
    }

    private fun getCurrentUserId(): String? {
        currentUser = auth.currentUser
        return currentUser?.uid
    }

    private fun openChatWithUser(user: ChatUser) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("userId", user.userId)
        intent.putExtra("name", user.name)
        Log.d("UserMessage", "Opening chat with user ID: ${user.userId}")
        startActivity(intent)
    }
}
