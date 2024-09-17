package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.capstone.peopleconnect.Client.Fragments.SettingsSecurityFragmentClient
import com.capstone.peopleconnect.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsSecurityFragmentSProvider : Fragment() {
    private var email: String? = null
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_security_s_provider, container, false)

        // Find the back button and set an OnClickListener
        val backButton: ImageView = view.findViewById(R.id.btnBackSProvider)
        backButton.setOnClickListener {
            // Navigate back to the HomeFragmentClient
            requireActivity().supportFragmentManager.popBackStack()
        }

        val submitButton: Button = view.findViewById(R.id.btnSubmit)
        submitButton.setOnClickListener {
            handlePasswordChange()
        }

        return view
    }

    private fun handlePasswordChange() {
        val currentPasswordEditText: EditText = view?.findViewById(R.id.currEt) ?: return
        val newPasswordEditText: EditText = view?.findViewById(R.id.newEt) ?: return
        val confirmPasswordEditText: EditText = view?.findViewById(R.id.confirmEt) ?: return

        val currentPassword = currentPasswordEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Password Mismatch, Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        email?.let { email ->
            val user = auth.currentUser

            user?.let { firebaseUser ->
                val credential = EmailAuthProvider.getCredential(email, currentPassword)

                firebaseUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        // Re-authentication successful, now update the password
                        firebaseUser.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Updated Password Successfully", Toast.LENGTH_SHORT).show()
                                requireActivity().supportFragmentManager.popBackStack()
                            } else {

                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Current Password Incorrect", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(email: String?) = SettingsSecurityFragmentSProvider().apply {
            arguments = Bundle().apply {
                putString("EMAIL", email)
            }
        }
    }
}