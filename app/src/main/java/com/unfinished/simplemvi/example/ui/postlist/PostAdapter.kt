package com.unfinished.simplemvi.example.ui.postlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unfinished.simplemvi.example.databinding.ViewPostBinding
import net.dean.jraw.models.Submission

class PostAdapter(
    private val posts: List<Submission>,
    private val postSelectedListener: (post: Submission) -> Unit
): RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply {
            this.binding.root.setOnClickListener {
                postSelectedListener.invoke(posts[this.layoutPosition])
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.titleTextView.text = post.title
        post.thumbnail?.let {
            Glide.with(holder.binding.root.context)
                .load(post.thumbnail)
                .centerCrop()
                .into(holder.binding.imageView)
        }
    }

    override fun getItemCount(): Int = posts.size

    inner class ViewHolder(val binding: ViewPostBinding): RecyclerView.ViewHolder(binding.root)
}