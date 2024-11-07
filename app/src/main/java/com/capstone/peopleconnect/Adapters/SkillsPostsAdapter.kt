import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.peopleconnect.Helper.ImagePreviewActivity
import com.capstone.peopleconnect.R
import com.squareup.picasso.Picasso

class SkillsPostsAdapter(
    private val postImages: List<String>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<SkillsPostsAdapter.SkillsPostsViewHolder>() {

    inner class SkillsPostsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillsPostsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.skills_posts_list, parent, false)
        return SkillsPostsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkillsPostsViewHolder, position: Int) {
        val imageUrl = postImages[position]
        Picasso.get().load(imageUrl).into(holder.cardImage)
        holder.itemView.setOnClickListener {
            onImageClick(imageUrl)
        }
    }

    override fun getItemCount(): Int = postImages.size
}
