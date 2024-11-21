package com.capstone.peopleconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.peopleconnect.Adapters.FeedbackOptionsSProviderAdapter
import com.capstone.peopleconnect.databinding.FragmentFeedbackSelectionSProviderBinding

class FeedbackSelectionFragmentSProvider : Fragment() {
    private var _binding: FragmentFeedbackSelectionSProviderBinding? = null
    private val binding get() = _binding!!
    private var rating: String? = null
    private var bookingId: String? = null
    private var selectedFeedback = listOf<String>()

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
        _binding = FragmentFeedbackSelectionSProviderBinding.inflate(inflater, container, false)
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
                "Very polite and respectful",
                "Clear communication",
                "Punctual and prepared",
                "Followed instructions well",
                "Pleasant to work with"
            )
            4f -> listOf(
                "Polite and friendly",
                "Good communication",
                "Generally on time",
                "Cooperative",
                "Easy to work with"
            )
            3f -> listOf(
                "Satisfactory behavior",
                "Adequate communication",
                "Mostly followed instructions",
                "Reasonable cooperation"
            )
            2f -> listOf(
                "Communication issues",
                "Delayed responses",
                "Unclear expectations",
                "Some difficulties in cooperation"
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

        binding.feedbackRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FeedbackOptionsSProviderAdapter(feedbackOptions) { selected ->
                selectedFeedback = selected
                binding.btnSubmitFeedback.isEnabled = selected.isNotEmpty()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSubmitFeedback.setOnClickListener {
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
        fun newInstance(rating: String, bookingId: String) =
            FeedbackSelectionFragmentSProvider().apply {
                arguments = Bundle().apply {
                    putString(ARG_RATING, rating)
                    putString(ARG_BOOKING_ID, bookingId)
                }
            }
    }
}