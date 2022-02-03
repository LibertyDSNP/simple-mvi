package com.unfinished.simplemvi.example.ui.postlist

import com.unfinished.simplemvi.Action
import com.unfinished.simplemvi.Presenter
import com.unfinished.simplemvi.State
import com.unfinished.simplemvi.example.repository.RedditRepository
import kotlinx.coroutines.flow.map
import net.dean.jraw.models.Submission

/**
 * subreddit is used to query the repository for posts of a specific subreddit. If subreddit is null, the repository will be queried for the most trending posts. subreddit should be set before calling [Presenter.trigger]. Otherwise, the most trending posts will be loaded instead.
 *
 */
class PostListPresenter(
    private val repository: RedditRepository
): Presenter<PostListState, PostListAction>() {
    var subreddit: String? = null

    override fun startingState(): PostListState = PostListState()

    override fun process(action: PostListAction) {
        when (action) {
            is PostListAction.LoadMorePosts -> loadMorePosts(false)
            is PostListAction.RefreshPosts -> loadMorePosts(true)
        }
    }

    private fun loadMorePosts(restart: Boolean) {
        subreddit?.let { subreddit ->
            collect(repository.moreSubredditPosts(subreddit, restart).map {
                PostListAction.NewPosts(it)
            })
        } ?: run {
            collect(repository.moreTrendingPosts(restart).map {
                PostListAction.NewPosts(it)
            })
        }
    }

    override fun reduce(lastState: PostListState, action: PostListAction): PostListState {
        return when (action) {
            is PostListAction.NewPosts -> lastState.copy(
                posts = lastState.posts + action.posts,
                loading = false
            )
            PostListAction.LoadMorePosts -> lastState.copy(
                loading = true
            )
            PostListAction.RefreshPosts -> lastState.copy(
                posts = emptyList(),
                loading = true
            )
        }
    }
}

data class PostListState (
    val loading: Boolean = false,
    val posts: List<Submission> = emptyList()
): State

sealed class PostListAction: Action {
    object RefreshPosts: PostListAction()
    object LoadMorePosts: PostListAction()
    data class NewPosts(val posts: List<Submission>): PostListAction()
}