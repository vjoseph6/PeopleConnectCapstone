package com.capstone.peopleconnect.Client.Welcome.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.capstone.peopleconnect.Client.Welcome.onboarding.screens.ClientFirstGetStarted
import com.capstone.peopleconnect.Client.Welcome.onboarding.screens.ClientSecondGetStarted
import com.capstone.peopleconnect.Client.Welcome.onboarding.screens.ClientThirdGetStarted
import com.capstone.peopleconnect.R
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ClientViewPageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ClientViewPageFragment : Fragment() {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_client_view_page, container, false)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val fragmentList = arrayListOf<Fragment>(
            ClientFirstGetStarted(),
            ClientSecondGetStarted(),
            ClientThirdGetStarted()
        )

        val adapter = ClientViewPageAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        viewPager.adapter = adapter

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ClientViewPageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
                ClientViewPageFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}