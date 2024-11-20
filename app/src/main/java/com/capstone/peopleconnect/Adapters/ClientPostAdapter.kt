package com.capstone.peopleconnect.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstone.peopleconnect.Classes.Post
import com.capstone.peopleconnect.R

class ClientPostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val isFromProvider: Boolean = false
) : RecyclerView.Adapter<ClientPostAdapter.PostViewHolder>() {

    private val posts = mutableListOf<Post>()

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_list_item, parent, false)
        return PostViewHolder(view, onPostClick, isFromProvider)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val onPostClick: (Post) -> Unit,
        private val isFromProvider: Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val singleImage: ImageView = itemView.findViewById(R.id.singleImage)
        private val twoImagesContainer: LinearLayout = itemView.findViewById(R.id.twoImagesContainer)
        private val threeImagesContainer: LinearLayout = itemView.findViewById(R.id.threeImagesContainer)

        fun bind(post: Post) {
            tvCategory.apply {
                text = post.categoryName
                if (isFromProvider) {
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.blue))
                }
            }
            tvDescription.text = post.postDescription
            tvDate.text = "Date: ${post.bookDay}"
            
            // Handle images based on count
            when (post.postImages.size) {
                1 -> displaySingleImage(post.postImages[0])
                2 -> displayTwoImages(post.postImages)
                3 -> displayThreeImages(post.postImages)
            }
            
            // Add click listener to the entire item
            itemView.setOnClickListener {
                onPostClick(post)
            }
        }

        private fun displaySingleImage(imageUrl: String) {
            singleImage.visibility = View.VISIBLE
            twoImagesContainer.visibility = View.GONE
            threeImagesContainer.visibility = View.GONE
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .into(singleImage)
        }

        private fun displayTwoImages(imageUrls: List<String>) {
            singleImage.visibility = View.GONE
            twoImagesContainer.visibility = View.VISIBLE
            threeImagesContainer.visibility = View.GONE

            val firstImage: ImageView = itemView.findViewById(R.id.firstImageTwo)
            val secondImage: ImageView = itemView.findViewById(R.id.secondImageTwo)

            Glide.with(itemView.context).load(imageUrls[0]).into(firstImage)
            Glide.with(itemView.context).load(imageUrls[1]).into(secondImage)
        }

        private fun displayThreeImages(imageUrls: List<String>) {
            singleImage.visibility = View.GONE
            twoImagesContainer.visibility = View.GONE
            threeImagesContainer.visibility = View.VISIBLE

            val firstImage: ImageView = itemView.findViewById(R.id.firstImageThree)
            val secondImage: ImageView = itemView.findViewById(R.id.secondImageThree)
            val thirdImage: ImageView = itemView.findViewById(R.id.thirdImageThree)

            Glide.with(itemView.context).load(imageUrls[0]).into(firstImage)
            Glide.with(itemView.context).load(imageUrls[1]).into(secondImage)
            Glide.with(itemView.context).load(imageUrls[2]).into(thirdImage)
        }
    }
} 