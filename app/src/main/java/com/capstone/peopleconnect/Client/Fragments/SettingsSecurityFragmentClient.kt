package com.capstone.peopleconnect.Client.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult

class SettingsSecurityFragmentClient : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_settings_security_client, container, false)

        val backButton: ImageView = view.findViewById(R.id.btnBackClient)
        backButton.setOnClickListener {
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
            // Show error: Password must be at least 6 characters
            return
        }

        if (newPassword != confirmPassword) {
            // Show error: New password and confirm password do not match
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
                                Toast.makeText(requireContext(), "Current Password Incorrect", Toast.LENGTH_SHORT).show()
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
        fun newInstance(email: String?) = SettingsSecurityFragmentClient().apply {
            arguments = Bundle().apply {
                putString("EMAIL", email)
            }
        }
    }
}
