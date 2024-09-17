package com.capstone.peopleconnect.Client.Fragments

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Classes.User
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.LocationFragmentSProvider
import com.capstone.peopleconnect.SelectAccount
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ProfileFragmentClient : Fragment() {

    private var firstName: String? = null
    private var middleName: String? = null
    private var lastName: String? = null
    private var userAddress: String? = null
    private var email: String? = null
    private var profileImageUrl: String? = null

    private lateinit var userQuery: Query
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            firstName = it.getString("FIRST_NAME")
            middleName = it.getString("MIDDLE_NAME")
            lastName = it.getString("LAST_NAME")
            userAddress = it.getString("USER_ADDRESS")
            email = it.getString("EMAIL")
            profileImageUrl = it.getString("PROFILE_IMAGE_URL")
        }

        email?.let {
            userQuery = FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("email").equalTo(it)
        } ?: run {
            // Handle case when email is null
            // Consider showing an error or using a default value
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfile(view)
    }

    override fun onStart() {
        super.onStart()

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.children.firstOrNull()
                    val user = data?.getValue(User::class.java)
                    user?.let {
                        updateUI(user)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
                // Consider showing a user-friendly message
            }
        }
        userQuery.addValueEventListener(valueEventListener)
    }

    override fun onStop() {
        super.onStop()
        userQuery.removeEventListener(valueEventListener)
    }

    private fun setupProfile(view: View) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val address: TextView = view.findViewById(R.id.tvLocation)
        val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)

        // Set default values or placeholders if needed
        tvName.text = ""
        address.text = ""
        tvEmail.text = ""
        ivProfileImage.setImageResource(R.drawable.profile) // Placeholder

        // Profile icons
        val profileIcons: LinearLayout = view.findViewById(R.id.profileMenuLayout)
        profileIcons.setOnClickListener {
            val profileFragment = MyProfileFragmentClient.newInstance(
                firstName = firstName,
                middleName = middleName,
                lastName = lastName ,
                email = email,
                profileImageUrl = profileImageUrl
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        // Location icons
        val locationIcons: LinearLayout = view.findViewById(R.id.locationMenuLayout)
        locationIcons.setOnClickListener {
            val locationFragment = LocationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, locationFragment)
                .addToBackStack(null)
                .commit()
        }

        // Security icons
        val securityIcons: LinearLayout = view.findViewById(R.id.securityMenuLayout_client)
        securityIcons.setOnClickListener {
            val securityFragment = SettingsSecurityFragmentClient.newInstance(email = email)
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, securityFragment)
                .addToBackStack(null)
                .commit()
        }

        val logoutIcons: LinearLayout = view.findViewById(R.id.logoutMenuLayout_client)
        logoutIcons.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.client_dialog_logout, null)
        val builder = AlertDialog.Builder(requireContext()).apply {
            setView(dialogView)
            setCancelable(false)
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
        dialog.show()

        dialogView.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), SelectAccount::class.java)
            startActivity(intent)
            activity?.finish() // Close the current activity
        }

        dialogView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }
    }
    private fun updateUI(user: User) {
        view?.let { view ->
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            val address: TextView = view.findViewById(R.id.tvLocation)
            val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)

            val fullName = "${user.firstName} ${user.middleName} ${user.lastName}".trim()
            tvName.text = fullName
            address.text = user.address
            tvEmail.text = user.email

            Picasso.get()
                .load(user.profileImageUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(ivProfileImage)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(firstName: String?, middleName: String?, lastName: String?, email: String?, profileImageUrl: String?) =
            ProfileFragmentClient().apply {
                arguments = Bundle().apply {
                    putString("FIRST_NAME", firstName)
                    putString("MIDDLE_NAME", middleName)
                    putString("LAST_NAME", lastName)
                    putString("EMAIL", email)
                    putString("PROFILE_IMAGE_URL", profileImageUrl)
                }
            }
    }
}
