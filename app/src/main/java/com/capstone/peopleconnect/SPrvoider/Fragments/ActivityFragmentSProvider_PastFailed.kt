package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.capstone.peopleconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * A simple [Fragment] subclass.
 * Use the [ActivityFragmentSProvider_PastFailed.newInstance] factory method to
 * create an instance of this fragment.
 */
class ActivityFragmentSProvider_PastFailed : Fragment() {

    // Parameters for fragment arguments
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
        return inflater.inflate(
            R.layout.fragment_activity_s_provider__past_failed,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView =
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // TextView for Successful tab
        val tvSuccessful: TextView = view.findViewById(R.id.tvSuccessful_Past_Failed)
        tvSuccessful.setOnClickListener {
            val pastSuccessfulFragment = ActivityFragmentSProvider_PastSuccessful()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_past_failed, pastSuccessfulFragment)
                .addToBackStack(null)
                .commit()

            // Optionally hide the bottom navigation bar
            bottomNavigationView?.visibility = View.GONE
        }

        // TextView for Present tab
        val tvPresent: TextView = view.findViewById(R.id.tvPresent_Past_Failed)
        tvPresent.setOnClickListener {
            val presentSuccessfulFragment = ActivityFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, presentSuccessfulFragment)
                .addToBackStack(null)
                .commit()

            // Optionally hide the bottom navigation bar
            bottomNavigationView?.visibility = View.GONE
        }
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ActivityFragmentSProvider_PastFailed.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivityFragmentSProvider_PastFailed().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
