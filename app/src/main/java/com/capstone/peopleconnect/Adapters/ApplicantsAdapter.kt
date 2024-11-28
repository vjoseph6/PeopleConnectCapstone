import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Classes.PostApplication
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ApplicantsAdapter(
    private val onActionClick: (PostApplication, Boolean) -> Unit,
    private val fetchUserDetails: (String, (String, String) -> Unit) -> Unit,
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

        holder.itemView.setOnClickListener {
            fetchUserDetails(application.providerEmail) { name, _ ->
                onApplicantClick(name, "fromApplicants")
            }
        }
    }

    override fun getItemCount() = applications.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val btnAccept: ImageButton = itemView.findViewById(R.id.btnAccept)
        private val btnReject: ImageButton = itemView.findViewById(R.id.btnReject)

        fun bind(application: PostApplication) {
            // Fetch user details
            fetchUserDetails(application.providerEmail) { name, profileImageUrl ->
                tvName.text = name
                if (profileImageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(profileImageUrl)
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(imgProfile)
                }
            }
            
            btnAccept.setOnClickListener { onActionClick(application, true) }
            btnReject.setOnClickListener { onActionClick(application, false) }
        }
    }
} 