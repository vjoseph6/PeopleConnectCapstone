package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.capstone.peopleconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ActivityFragmentSProvider_PresentFailed.newInstance] factory method to
 * create an instance of this fragment.
 */
class ActivityFragmentSProvider_PresentFailed : Fragment() {
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
        return inflater.inflate(
            R.layout.fragment_activity_s_provider__present_failed,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        // Find the TextView by ID
//        val tvBookingFailed: TextView = view.findViewById(R.id.tvBooking_Failed)
//
//        // Set an onClickListener to handle the fragment transaction
//        tvBookingFailed.setOnClickListener {
//            // Create the new fragment instance
//            val fragment = ActivityFragmentSProvider.newInstance("param1", "param2")
//
//            // Perform the fragment transaction to replace the current fragment
//            val transaction = parentFragmentManager.beginTransaction()
//            transaction.replace(R.id.fragment_container_present_failed, fragment) // Make sure to use the right container ID
//            transaction.addToBackStack(null)  // This ensures that the user can navigate back
//            transaction.commit()
//        }
//
//        // Handling tvFailed click
//        val tvFailedSuccessful: TextView = view.findViewById(R.id.tvFailed_Successful)
//
//        // Set an onClickListener to handle the fragment transaction
//        tvFailedSuccessful.setOnClickListener {
//
//            // Create an instance of the new fragment for failed activities
//            val fragment = ActivityFragmentSProvider_PresentSuccessful.newInstance("param1", "param2")
//
//            // Perform the fragment transaction to replace the current fragment
//            val transaction = parentFragmentManager.beginTransaction()
//            transaction.replace(R.id.fragment_container_present_failed, fragment) // Make sure to use the right container ID
//            transaction.addToBackStack(null)  // This ensures that the user can navigate back
//            transaction.commit()
//        }

        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Change the type to TextView instead of LinearLayout
        val tvBooking: TextView = view.findViewById(R.id.tvBooking_Failed)
        tvBooking.setOnClickListener {
            val presentBookingFragment = ActivityFragmentSProvider()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_present_failed, presentBookingFragment)
                .addToBackStack(null)
                .commit()

//            // Hide the bottom navigation bar
//            bottomNavigationView?.visibility = View.GONE

        }

        // Change the type to TextView instead of LinearLayout
        val tvSuccessful: TextView = view.findViewById(R.id.tvSuccessful_Failed)
        tvSuccessful.setOnClickListener {
            val presentSuccessfulFragment = ActivityFragmentSProvider_PresentSuccessful()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_present_failed, presentSuccessfulFragment)
                .addToBackStack(null)
                .commit()

//            // Hide the bottom navigation bar
//            bottomNavigationView?.visibility = View.GONE

        }



    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ActivityFragmentSProvider_PresentFailed.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivityFragmentSProvider_PresentFailed().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}