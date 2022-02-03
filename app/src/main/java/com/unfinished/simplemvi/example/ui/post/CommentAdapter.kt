package com.unfinished.simplemvi.example.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unfinished.simplemvi.example.R
import com.unfinished.simplemvi.example.databinding.ViewCommentBinding
import com.unfinished.simplemvi.example.model.PostComment

class CommentAdapter(
    private val comments: List<PostComment>
): RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ViewCommentBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply {
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.authorNameTextView.text = comment.author
        holder.binding.createdDateTextView.text = comment.timestamp.toString()
        holder.binding.subjectTextView.text = comment.text
        holder.binding.dividerContainer.removeAllViews()
        val layoutInflater = LayoutInflater.from(holder.binding.root.context)
        for (i in 0..comment.depth) {
            holder.binding.dividerContainer.addView(layoutInflater.inflate(R.layout.view_divider, holder.binding.dividerContainer, false))
        }
    }

    override fun getItemCount(): Int = comments.size
}