package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.capstone.peopleconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragmentClient.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragmentClient : Fragment() {
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
        return inflater.inflate(R.layout.fragment_profile_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inside the onViewCreated method
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Profile icons
        val profileIcons: LinearLayout = view.findViewById(R.id.profileMenuLayout)
        profileIcons.setOnClickListener {
            val profileFragment = MyProfileFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, profileFragment)
                .addToBackStack(null)
                .commit()

            // Hide the bottom navigation bar
            bottomNavigationView?.visibility = View.GONE
        }

        // Location icons
        val locationIcons: LinearLayout = view.findViewById(R.id.locationMenuLayout)
        locationIcons.setOnClickListener {
            val locationFragment = LocationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, locationFragment)
                .addToBackStack(null)
                .commit()

            // Hide the bottom navigation bar
            bottomNavigationView?.visibility = View.GONE
        }

        // Settings icons
        val settingsIcons: LinearLayout = view.findViewById(R.id.settingsMenuLayout)
        settingsIcons.setOnClickListener {
            val settingsFragment = SettingsFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, settingsFragment)
                .addToBackStack(null)
                .commit()

            // Hide the bottom navigation bar
            bottomNavigationView?.visibility = View.GONE
        }

        // Logout button click listener
        val logoutButton: LinearLayout = view.findViewById(R.id.logoutMenuLayout)
        logoutButton.setOnClickListener {
            showLogoutDialog()
        }

    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.client_dialog_logout, null)
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setView(dialogView)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)
        val tvCancel = dialogView.findViewById<TextView>(R.id.tvCancel)
        val cbRememberLogin = dialogView.findViewById<CheckBox>(R.id.cbRememberLogin)

        btnLogout.setOnClickListener {
            // Perform logout action
            alertDialog.dismiss()
            // Add your logout logic here
        }

        tvCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragmentClient.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragmentClient().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}