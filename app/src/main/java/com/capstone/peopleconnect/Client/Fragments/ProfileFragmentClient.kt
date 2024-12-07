package com.capstone.peopleconnect.Client.Fragments

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import com.capstone.peopleconnect.SiriScopeFragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
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
    private var serviceType: String? = null
    private var profileImageUrl: String? = null
    private lateinit var userQuery: Query
    private lateinit var valueEventListener: ValueEventListener
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

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
        return inflater.inflate(R.layout.fragment_profile_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfile(view)

        arguments?.let { args ->
            val target = args.getString("target")
            serviceType = args.getString("serviceType")
            val intent = args.getString("intent")

            if (intent == "add_post") {
                Handler().postDelayed({
                    if (serviceType.isNullOrEmpty() || serviceType == "Service Type not found") {
                        navigateToPost()
                    } else if (target == "add_post") {
                        Handler().postDelayed({
                            if (serviceType.isNullOrEmpty() || serviceType == "Service Type not found") {
                                navigateToPost()
                            } else {
                                navigateToPost()
                            }
                        }, 500)
                    }
                }, 500)
            }
        }
    }

    private fun navigateToPost() {

        val email = email
        val intent = arguments?.getString("intent") ?: arguments?.getString("target")

        val securityFragment = ManagePostFragment.newInstance(email, serviceType, intent)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, securityFragment)
            .addToBackStack(null)
            .commit()
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
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val address: TextView = view.findViewById(R.id.tvLocation)
        val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)

        // Set default values or placeholders if needed
        tvName.text = "Loading..."
        address.text = "Loading..."
        tvEmail.text = "Loading..."
        ivProfileImage.setImageResource(R.drawable.profile) // Placeholder

        // Profile icons
        val profileIcons: LinearLayout = view.findViewById(R.id.profileMenuLayout)
        profileIcons.setOnClickListener {
            val profileFragment = MyProfileFragmentClient.newInstance(
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
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
        val postIcons: LinearLayout = view.findViewById(R.id.postMenuLayout_client)
        postIcons.setOnClickListener {
            val securityFragment = ManagePostFragment.newInstance(email = email,
                serviceType = arguments?.getString("serviceType").toString(), intent = arguments?.getString("intent").toString())
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, securityFragment)
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

        val scopeIcons: LinearLayout = view.findViewById(R.id.scopeLayout_client)
        scopeIcons.setOnClickListener {
            val securityFragment = SiriScopeFragment.newInstance(email = email)
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
            // Sign out from Firebase Auth
            auth.signOut()

            // Clear any stored user data
            val sharedPref = requireActivity().getSharedPreferences(
                "com.capstone.peopleconnect.PREFERENCE_FILE_KEY",
                Context.MODE_PRIVATE
            )
            sharedPref.edit().clear().apply()

            // Navigate to SelectAccount activity
            val intent = Intent(requireContext(), SelectAccount::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
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
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvEmail: TextView = view.findViewById(R.id.tvEmail)
            val address: TextView = view.findViewById(R.id.tvLocation)
            val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)

            val fullName = "${user.firstName ?: ""} ${user.middleName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { "No Name" }
            tvName.text = fullName
            tvEmail.text = user.email
            address.text = user.address.trim().ifEmpty { "No Address" }

            // Check if profileImageUrl is not null or empty
            if (!profileImageUrl.isNullOrEmpty()) {
                // If the URL is valid, load the image
                Picasso.get()
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile)  // Placeholder image
                    .error(R.drawable.profile)        // Error image
                    .into(ivProfileImage)
            } else {
                // If profileImageUrl is empty or null, set a placeholder image
                ivProfileImage.setImageResource(R.drawable.profile)  // Placeholder image
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
            ivProfileImage.setImageResource(R.drawable.profile) // Placeholder image
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(firstName: String?, middleName: String?, lastName: String?, email: String?, profileImageUrl: String?, address:String?) =
            ProfileFragmentClient().apply {
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
