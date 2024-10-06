package com.capstone.peopleconnect.SProvider.Fragments

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
import com.capstone.peopleconnect.Client.Fragments.ProfileFragmentClient
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.Fragments.LocationFragmentSProvider
import com.capstone.peopleconnect.SPrvoider.Fragments.SettingsSecurityFragmentSProvider
import com.capstone.peopleconnect.SelectAccount
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ProfileFragmentSProvider : Fragment() {

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
            email = it.getString("EMAIL")
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_s_provider, container, false)
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
                } else {
                    updateUIWithPlaceholders()
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
        val tvName: TextView = view.findViewById(R.id.tvName_sprovider)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail_sprovider)
        val address: TextView = view.findViewById(R.id.tvLocation_sprovider)
        val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage_sprovider)

        // Set default values or placeholders if needed
        tvName.text = "Name"
        address.text = "Adress"
        tvEmail.text = "Email"
        ivProfileImage.setImageResource(R.drawable.profile1) // Placeholder

        // Profile icons
        val profileIcons: LinearLayout = view.findViewById(R.id.profileMenuLayout_sprovider)
        profileIcons.setOnClickListener {
            val profileFragment = MyProfileFragmentSProvider.newInstance(
                firstName = firstName,
                middleName = middleName,
                lastName = lastName ,
                email = email,
                profileImageUrl = profileImageUrl,
                address = userAddress
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        // Security icons
        val securityIcons: LinearLayout = view.findViewById(R.id.securityMenuLayout_sprovider)
        securityIcons.setOnClickListener {
            val securityFragment = SettingsSecurityFragmentSProvider.newInstance(email = email)
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, securityFragment)
                .addToBackStack(null)
                .commit()
        }

        // Logout icons
        val logoutIcons: LinearLayout = view.findViewById(R.id.logoutMenuLayout_sprovider)
        logoutIcons.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.sprovider_dialog_logout, null)
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
        firstName = user.firstName
        middleName = user.middleName
        lastName = user.lastName
        userAddress = user.address
        profileImageUrl = user.profileImageUrl

        view?.let { view ->
            val tvName: TextView = view.findViewById(R.id.tvName_sprovider)
            val tvEmail: TextView = view.findViewById(R.id.tvEmail_sprovider)
            val address: TextView = view.findViewById(R.id.tvLocation_sprovider)
            val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage_sprovider)

            val fullName = "${user.firstName ?: ""} ${user.middleName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { "No Name" }
            tvName.text = fullName
            tvEmail.text = user.email
            address.text = user.address.trim().ifEmpty { "No Address" }

            // Check if profileImageUrl is not null or empty
            if (!profileImageUrl.isNullOrEmpty()) {
                // If the URL is valid, load the image
                Picasso.get()
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile1)  // Placeholder image
                    .error(R.drawable.profile1)        // Error image
                    .into(ivProfileImage)
            } else {
                // If profileImageUrl is empty or null, set a placeholder image
                ivProfileImage.setImageResource(R.drawable.profile1)  // Placeholder image
            }
        }
    }

    private fun updateUIWithPlaceholders() {
        view?.let { view ->
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            val address: TextView = view.findViewById(R.id.tvLocation)
            val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)

            tvName.text = "No Name"
            address.text = "No Address"
            tvEmail.text = "No Email Provided"
            ivProfileImage.setImageResource(R.drawable.profile1) // Placeholder image
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(firstName: String?, middleName: String?, lastName: String?, email: String?, profileImageUrl: String?, address:String?) =
            ProfileFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("FIRST_NAME", firstName)
                    putString("MIDDLE_NAME", middleName)
                    putString("LAST_NAME", lastName)
                    putString("EMAIL", email)
                    putString("PROFILE_IMAGE_URL", profileImageUrl)
                    putString("ADDRESS", address)
                }
            }
    }
}
