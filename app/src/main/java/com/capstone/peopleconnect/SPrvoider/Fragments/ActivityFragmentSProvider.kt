package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * Use the [ActivityFragmentSProvider.newInstance] factory method to
 * create an instance of this fragment.
 */
class ActivityFragmentSProvider : Fragment() {
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
        return inflater.inflate(R.layout.fragment_activity_s_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Change the type to TextView instead of LinearLayout
        val tvSuccessful: TextView = view.findViewById(R.id.tvSuccessful_Present)
        tvSuccessful.setOnClickListener {
            val presentSuccessfulFragment = ActivityFragmentSProvider_PresentSuccessful()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, presentSuccessfulFragment)
                .addToBackStack(null)
                .commit()

//            // Hide the bottom navigation bar
//            bottomNavigationView?.visibility = View.GONE

        }

        // Change the type to TextView instead of LinearLayout
        val tvFailed: TextView = view.findViewById(R.id.tvFailed_Present)
        tvFailed.setOnClickListener {
            val presentFailedFragment = ActivityFragmentSProvider_PresentFailed()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, presentFailedFragment)
                .addToBackStack(null)
                .commit()

//            // Hide the bottom navigation bar
//            bottomNavigationView?.visibility = View.GONE

        }

        // Change the type to TextView instead of LinearLayout
        val tvPast: TextView = view.findViewById(R.id.tvPast_Present)
        tvPast.setOnClickListener {
            val pastSuccessfulFragment = ActivityFragmentSProvider_PastSuccessful()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, pastSuccessfulFragment)
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
         * @return A new instance of fragment ActivityFragmentSProvider.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivityFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}