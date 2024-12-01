import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.PostApplication
import com.capstone.peopleconnect.R
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ApplicantsAdapter(
    private val onActionClick: (PostApplication, Boolean) -> Unit,
    private val fetchUserDetails: (String, (String,String, String, String, Double, Double) -> Unit) -> Unit,
    private val onApplicantClick: (String, String) -> Unit
) : RecyclerView.Adapter<ApplicantsAdapter.ViewHolder>() {
    private var applications = mutableListOf<PostApplication>()

    fun setApplications(newApplications: List<PostApplication>) {
        applications.clear()
        applications.addAll(newApplications)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_applicant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val application = applications[position]
        holder.bind(application)
    }

    override fun getItemCount() = applications.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProfile: ShapeableImageView = itemView.findViewById(R.id.imgProfile)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val providerRating: RatingBar = itemView.findViewById(R.id.providerRating)
        private val providerCategory: TextView = itemView.findViewById(R.id.providerCategory)
        private val providerDescription: TextView = itemView.findViewById(R.id.providerDescription)
        private val providerPrice: TextView = itemView.findViewById(R.id.providerPrice)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val btnAccepted: Button = itemView.findViewById(R.id.btnAccepted)

        fun bind(application: PostApplication) {
            // Fetch detailed user and skill information
            fetchUserDetails(application.providerEmail) {
                    skillName,name, profileImageUrl, description, skillRate, rating ->

                // Set name
                tvName.text = name

                // Set profile image
                if (profileImageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(profileImageUrl)
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(imgProfile)
                }

                // Set description
                providerDescription.text = description

                // Set price
                providerPrice.text = "₱${String.format("%.2f", skillRate)}"

                providerRating.rating = String.format("%.1f", rating).toFloat()

                providerCategory.text = skillName
            }

            if (application.status == "Accepted") {
                // Hide accept and reject buttons, show accepted button
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnAccepted.visibility = View.VISIBLE
            } else {
                // Show accept and reject buttons, hide accepted button
                btnAccept.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnAccepted.visibility = View.GONE

                // Set click listeners for accept and reject
                btnAccept.setOnClickListener {
                    onActionClick(application, true)
                }
                btnReject.setOnClickListener {
                    onActionClick(application, false)
                }
            }
            // Set item click listener to navigate to provider profile
            itemView.setOnClickListener {
                fetchUserDetails(application.providerEmail) {
                        skillName, name, profileImageUrl, description, skillRate, rating ->

                    val tag = if (application.status == "Accepted") null else "fromApplicants"
                    onApplicantClick(name, tag.toString())
                }
            }
        }
    }
}