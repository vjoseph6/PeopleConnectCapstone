package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.capstone.peopleconnect.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NotificationFragmentClient.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationFragmentClient : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_notification_client, container, false)

        // Find the back button and set an OnClickListener
        val backButton: ImageView = view.findViewById(R.id.back_button_notify)
        backButton.setOnClickListener {
            // Navigate back to the HomeFragmentClient
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NotificationFragmentClient.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotificationFragmentClient().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

//  Please do not delete this, as this code is the connection for the notification. It's just missing something, which is why it isn't functioning yet.

//package com.capstone.peopleconnect.Client.Fragments
//
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.recyclerview.widget.DividerItemDecoration
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.capstone.peopleconnect.Message.chat.ChatActivity
//import com.capstone.peopleconnect.Notifications.adapter.NotificationAdapter
//import com.capstone.peopleconnect.Notifications.model.NotificationModel
//import com.capstone.peopleconnect.R
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//
//class NotificationFragmentClient : Fragment() {
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var notificationAdapter: NotificationAdapter
//    private lateinit var database: FirebaseDatabase
//    private lateinit var notificationsRef: DatabaseReference
//    private var notificationListener: ValueEventListener? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_notification_client, container, false)
//
//        // Initialize Firebase
//        database = FirebaseDatabase.getInstance()
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        if (currentUser != null) {
//            notificationsRef = database.reference
//                .child("notifications")
//                .child(currentUser.uid)
//        }
//
//        // Initialize views
//        recyclerView = view.findViewById(R.id.notification_recycler_view)
//
//        setupRecyclerView()
//        setupBackButton(view)
//        loadNotifications()
//
//        return view
//    }
//
//    private fun setupRecyclerView() {
//        notificationAdapter = NotificationAdapter(requireContext()) { notification ->
//            handleNotificationClick(notification)
//        }
//
//        recyclerView.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = notificationAdapter
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
//        }
//    }
//
//    private fun setupBackButton(view: View) {
//        view.findViewById<ImageView>(R.id.back_button_notify).setOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }
//    }
//
//    private fun loadNotifications() {
//        notificationListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val notifications = mutableListOf<NotificationModel>()
//
//                for (notificationSnapshot in snapshot.children) {
//                    notificationSnapshot.getValue(NotificationModel::class.java)?.let {
//                        notifications.add(it)
//                    }
//                }
//
//                // Sort notifications by timestamp (newest first)
//                notifications.sortByDescending { it.timestamp }
//
//                // Update UI
//                if (notifications.isEmpty()) {
//                    view?.findViewById<TextView>(R.id.no_notifications_text_client)?.visibility = View.VISIBLE
//                    recyclerView.visibility = View.GONE
//                } else {
//                    view?.findViewById<TextView>(R.id.no_notifications_text_client)?.visibility = View.GONE
//                    recyclerView.visibility = View.VISIBLE
//                    notificationAdapter.updateNotifications(notifications)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("NotificationFragment", "Error loading notifications: ${error.message}")
//            }
//        }
//
//        notificationsRef.addValueEventListener(notificationListener!!)
//    }
//
//    // Update the handleNotificationClick function
//    private fun handleNotificationClick(notification: NotificationModel) {
//        // Mark notification as read
//        notification.id.let { notificationId ->
//            notificationsRef.child(notificationId).child("isRead").setValue(true)
//        }
//
//        when (notification.type) {
//            "chat" -> {
//                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
//                    putExtra("userId", notification.senderId)
//                    putExtra("name", notification.senderName)
//                    notification.channelId?.let { putExtra("channelId", it) }
//                }
//                startActivity(intent)
//            }
//            "call" -> {
//                try {
//                    // Use the callLink from the notification
//                    notification.callLink?.let { callLink ->
//                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(callLink))
//                        startActivity(intent)
//                        Toast.makeText(context, "Joining video call...", Toast.LENGTH_SHORT).show()
//                    } ?: run {
//                        Toast.makeText(context, "Call link not found", Toast.LENGTH_SHORT).show()
//                    }
//                } catch (e: Exception) {
//                    Log.e("NotificationFragment", "Error opening call link", e)
//                    Toast.makeText(context, "Unable to join call", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        // Remove the Firebase listener
//        notificationListener?.let {
//            notificationsRef.removeEventListener(it)
//        }
//    }
//}