package com.capstone.peopleconnect.SPrvoider.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.capstone.peopleconnect.R

class SkillsPostFragmentSProvider : Fragment() {

    private var email: String? = null
    private var categoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("EMAIL")
            categoryName = it.getString("CATEGORY_NAME")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout first
        val view = inflater.inflate(R.layout.fragment_skills_post_s_provider, container, false)

        // Now you can safely access the TextView
        val textView: TextView = view.findViewById(R.id.tvSkills) // Replace with your TextView ID
        textView.text = categoryName // Set the category name

        val addPostBtn: ImageButton = view.findViewById(R.id.addPostBtn)
        addPostBtn.setOnClickListener {
            val newFragment = AddPostFragment.newInstance(email = email.toString(), categoryName = categoryName.toString())
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.frame_layout, newFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }


    companion object {

        @JvmStatic
        fun newInstance(email: String?, categoryName: String?) =
            SkillsPostFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString("EMAIL", email)
                    putString("CATEGORY_NAME", categoryName)
                }
            }
    }
}