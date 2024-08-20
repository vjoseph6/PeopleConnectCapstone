package com.capstone.peopleconnect.Client.Fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.capstone.peopleconnect.R
import com.google.android.material.bottomsheet.BottomSheetDialog

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragmentClient.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragmentClient : Fragment() {
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
        return inflater.inflate(R.layout.fragment_home_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Assume ivFilter is the ImageView for the filter icon
        val ivFilter: ImageView = view.findViewById(R.id.ivFilter)
        ivFilter.setOnClickListener {
            showFilterDialog()
        }

        // Notification icons
        val notificationIcons: LinearLayout = view.findViewById(R.id.notificationLayout)
        notificationIcons.setOnClickListener {
            val notificationFragment = NotificationFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, notificationFragment)
                .addToBackStack(null)
                .commit()

        }

        // Message icons
        val messageIcons: LinearLayout = view.findViewById(R.id.messageLayout)
        messageIcons.setOnClickListener {
            val messageFragment = MessageFragmentClient()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, messageFragment)
                .addToBackStack(null)
                .commit()

        }


    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.client_dialog_filter_options, null)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(dialogView)

        // Find buttons
        val btnCooking = dialogView.findViewById<Button>(R.id.btnCooking)
        val btnHomeCleaning = dialogView.findViewById<Button>(R.id.btnHomeCleaning)
        val btnLaundry = dialogView.findViewById<Button>(R.id.btnLaundry)

        // Function to handle button click
        fun handleButtonClick(button: Button) {
            // Set button background to green
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))

            // Delay for 200 milliseconds to allow the user to see the green background
            button.postDelayed({
                bottomSheetDialog.dismiss()  // Dismiss dialog and return to home
            }, 200)
        }

        // Setting up click listeners for each button
        btnCooking.setOnClickListener {
            handleButtonClick(btnCooking)
        }

        btnHomeCleaning.setOnClickListener {
            handleButtonClick(btnHomeCleaning)
        }

        btnLaundry.setOnClickListener {
            handleButtonClick(btnLaundry)
        }

        bottomSheetDialog.show()
    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragmentClient.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragmentClient().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}