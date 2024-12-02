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
import com.google.firebase.database.FirebaseDatabase
import android.widget.Toast
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.view.View.GONE
import android.view.View.VISIBLE
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.capstone.peopleconnect.Client.AddPostClientFragment

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

    inner class PostViewHolder(
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
        private val tvRemovePost: TextView = itemView.findViewById(R.id.tvRemovePost)
        private val btnEditPost: TextView = itemView.findViewById(R.id.tvEditPost)

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

            // Show or hide the remove post option based on the context
            if (isFromProvider) {
                btnEditPost.visibility = GONE
                tvRemovePost.visibility = GONE // Hide the remove post option
            } else {
                btnEditPost.visibility = VISIBLE
                tvRemovePost.visibility = VISIBLE // Show the remove post option
            }

            // Handle remove post click
            tvRemovePost.setOnClickListener {
                showRemovePostDialog(post)
            }

            // Handle edit post click
            btnEditPost.setOnClickListener {
                val fragment = AddPostClientFragment.newInstance(post.email.toString(),post)
                val transaction = itemView.context as AppCompatActivity
                transaction.supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        private fun showRemovePostDialog(post: Post) {
            val dialogView = LayoutInflater.from(itemView.context).inflate(R.layout.client_dialog_logout, null)
            val dialogBuilder = AlertDialog.Builder(itemView.context)
                .setView(dialogView)
                .setCancelable(false)

            val tvLogoutTitle = dialogView.findViewById<TextView>(R.id.tvLogoutTitle)
            tvLogoutTitle.text = "Are you sure you want to remove this post?"

            val dialog = dialogBuilder.create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(0)) // Make background transparent
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation // Apply animations
            dialog.show()

            // Ensure this is a Button, not a TextView
            val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)
            btnLogout.text = "Yes, remove post"
            btnLogout.setTextAppearance(itemView.context, R.style.BlueButtonStyle)  // Apply style correctly

            val btnCancel = dialogView.findViewById<TextView>(R.id.tvCancel)
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnLogout.setOnClickListener {
                removePost(post)
                dialog.dismiss() // Dismiss the dialog after confirming
            }
        }

        private fun removePost(post: Post) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("posts")
            databaseRef.child(post.postId).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Optionally, remove the post from the local list and notify the adapter
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        posts.removeAt(position)
                        notifyItemRemoved(position)
                        Toast.makeText(itemView.context, "Removed post successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle failure
                    Toast.makeText(itemView.context, "Failed to remove post", Toast.LENGTH_SHORT).show()
                }
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