package com.capstone.peopleconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.peopleconnect.Adapters.FeedbackOptionsAdapter
import com.capstone.peopleconnect.Classes.Rating
import com.capstone.peopleconnect.databinding.FragmentFeedbackSelectionBinding
import androidx.core.os.bundleOf


class FeedbackSelectionFragment : Fragment() {
    private var _binding: FragmentFeedbackSelectionBinding? = null
    private val binding get() = _binding!!
    private var rating: String? = null
    private var bookingId: String? = null
    private var selectedFeedback = listOf<String>()  // Changed to match service provider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            rating = it.getString(ARG_RATING)
            bookingId = it.getString(ARG_BOOKING_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val feedbackOptions = when (rating?.toFloatOrNull()) {
            5f -> listOf(
                "Professional and courteous",
                "Excellent communication",
                "On-time service",
                "Great attention to detail",
                "Went above and beyond"
            )
            4f -> listOf(
                "Very professional service",
                "Good communication",
                "Punctual service",
                "Careful attention to work",
                "Met all expectations"
            )
            3f -> listOf(
                "Acceptable service",
                "Basic communication",
                "Reasonable timing",
                "Standard work quality",
                "Met basic requirements"
            )
            2f -> listOf(
                "Service needs improvement",
                "Communication gaps",
                "Timing issues",
                "Work quality concerns",
                "Below expectations"
            )
            else -> emptyList()
        }

        binding.feedbackTitle.text = when (rating?.toFloatOrNull()) {
            5f -> "What made it perfect?"
            4f -> "What made it great?"
            3f -> "What made it good?"
            2f -> "What could be improved?"
            else -> "Select Feedback"
        }

        binding.feedbackOptionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FeedbackOptionsAdapter(feedbackOptions) { selected ->
                selectedFeedback = selected
                binding.btnSubmit.isEnabled = selected.isNotEmpty()
            }
        }

        // Initially disable submit button
        binding.btnSubmit.isEnabled = false
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSubmit.setOnClickListener {
            val feedback = selectedFeedback.joinToString(", ")
            parentFragmentManager.setFragmentResult("feedback_result", bundleOf("feedback" to feedback))
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RATING = "rating"
        private const val ARG_BOOKING_ID = "booking_id"

        @JvmStatic
        fun newInstance(bookingId: String, rating: String) =
            FeedbackSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                    putString(ARG_RATING, rating)
                }
            }
    }
}