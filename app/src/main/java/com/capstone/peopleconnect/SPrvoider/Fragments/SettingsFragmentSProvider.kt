package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.capstone.peopleconnect.Client.Fragments.SettingsHelpFragmentClient
import com.capstone.peopleconnect.Client.Fragments.SettingsSecurityFragmentClient
import com.capstone.peopleconnect.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragmentSProvider.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragmentSProvider : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_settings_s_provider, container, false)

        // Find the back button and set an OnClickListener
        val backButton: ImageView = view.findViewById(R.id.btnBackSProvider)
        backButton.setOnClickListener {
            // Navigate back to the HomeFragmentClient
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // security icons
        val securityIcons: LinearLayout = view.findViewById(R.id.security_option)
        securityIcons.setOnClickListener {
            val securityFragment = SettingsSecurityFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_layout_sprovider, securityFragment)
                .addToBackStack(null)
                .commit()

        }

        // help icons
        val helpIcons: LinearLayout = view.findViewById(R.id.help_option)
        helpIcons.setOnClickListener {
            val helpFragment = SettingsHelpFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_layout_sprovider, helpFragment)
                .addToBackStack(null)
                .commit()

        }


    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragmentSProvider.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}