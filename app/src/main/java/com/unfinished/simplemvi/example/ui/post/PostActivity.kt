package com.unfinished.simplemvi.example.ui.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.unfinished.simplemvi.Renderer
import com.unfinished.simplemvi.example.databinding.ActivityPostBinding
import com.unfinished.simplemvi.example.model.PostComment
import org.koin.android.viewmodel.ext.android.viewModel

private const val EXTRA_POST_ID = "postID"

/**
 * This shows how to use Simple MVI with an [Activity].
 * The [Activity] renders a post identified by a post id. The post id is set on the [com.unfinished.simplemvi.Presenter] where it is used to query the repository.
 *
 */
class PostActivity: AppCompatActivity(), Renderer<PostState> {
    lateinit var binding: ActivityPostBinding
    private val presenter: PostPresenter by viewModel()
    private val comments = mutableListOf<PostComment>()

    companion object {
        fun createIntent(context: Context, postId: String) = Intent(context, PostActivity::class.java).apply {
            putExtra(EXTRA_POST_ID, postId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = CommentAdapter(comments)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        requireNotNull(intent.getStringExtra(EXTRA_POST_ID))
        // If you want to use the post id in the Presenter.source() method to query the repository, you should set it on the presenter before calling Presenter.register() so that post id is available when Presenter.source() is being called.
        presenter.postId = intent.getStringExtra(EXTRA_POST_ID)!!
        presenter.register(this)
    }

    override fun render(state: PostState) {
        val post = state.post?.submission
        binding.titleTextView.text = post?.title ?: ""
        // Update the list of comments (that is used in the RecyclerView.Adapter) with the latest data from the state.
        comments.clear()
        state.post?.comments?.let {
            comments.addAll(it)
        }
        binding.recyclerView.adapter?.notifyDataSetChanged()

        binding.progressBar.visibility = if (state.post != null) {
            View.GONE
        } else {
            View.VISIBLE
        }

        post?.preview?.images?.firstOrNull()?.source?.url?.let {
            Glide.with(this)
                .load(it)
                .centerCrop()
                .into(binding.imageView)
        }
    }
}