package com.unfinished.simplemvi.example.ui.postlist

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unfinished.simplemvi.Renderer
import com.unfinished.simplemvi.example.databinding.FragmentPostListBinding
import com.unfinished.simplemvi.example.ui.post.PostActivity
import net.dean.jraw.models.Submission
import org.koin.android.viewmodel.ext.android.viewModel

private const val ARG_SUBREDDIT = "subreddit"

/**
 * This shows how to use Simple MVI with an [Activity].
 * This [Activity] shows a list of reddit posts. If a subreddit name is provided, it will show the posts of that subreddit, otherwise, the most rending posts will be loaded.
 * If available, the subreddit name is set on the [com.unfinished.simplemvi.Presenter] where it is used to query the repository.
 *
 */
class PostListFragment: Fragment(), Renderer<PostListState> {
    val presenter: PostListPresenter by viewModel()
    lateinit var binding: FragmentPostListBinding
    val posts = mutableListOf<Submission>()

    companion object {
        fun newInstance(subreddit: String? = null) = PostListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SUBREDDIT, subreddit)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.adapter = PostAdapter(posts) {
            startActivity(PostActivity.createIntent(requireContext(), it.id))
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() == posts.size - 1) {
                    presenter.trigger(PostListAction.LoadMorePosts)
                }
            }
        })
        binding.swipeRefreshLayout.setOnRefreshListener {
            presenter.trigger(PostListAction.RefreshPosts)
        }

        // If you want to use the subreddit name in the Presenter.source() method to query the repository, you should set it on the presenter before calling presenter.trigger(PostListAction.LoadMorePosts) so that it's available when Presenter.process() is being called.
        val subreddit =  arguments?.getString(ARG_SUBREDDIT)
        presenter.subreddit = subreddit
        presenter.register(this)

        presenter.trigger(PostListAction.RefreshPosts)
    }

    override fun render(state: PostListState) {
        // Update the list of posts (that is used in the RecyclerView.Adapter) with the latest data from the state.
        posts.clear()
        posts.addAll(state.posts)
        binding.recyclerView.adapter?.notifyDataSetChanged()
        binding.swipeRefreshLayout.isRefreshing = state.loading
    }
}