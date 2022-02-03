package com.unfinished.simplemvi.example.repository

import android.content.Context
import android.util.Log
import com.unfinished.simplemvi.example.model.Post
import com.unfinished.simplemvi.example.model.PostComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import net.dean.jraw.android.*
import net.dean.jraw.http.LogAdapter
import net.dean.jraw.http.SimpleHttpLogger
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.oauth.AccountHelper
import net.dean.jraw.oauth.StatefulAuthHelper
import net.dean.jraw.pagination.DefaultPaginator
import net.dean.jraw.tree.CommentNode
import net.dean.jraw.tree.RootCommentNode
import java.util.*

class RedditRepository(
    context: Context
) {

    private var accountHelper: AccountHelper
    private var tokenStore: SharedPreferencesTokenStore
    private var helper: StatefulAuthHelper? = null
    private var homePaginator: DefaultPaginator<Submission>? = null
    private var subredditPaginatorWrapper: SubredditPaginatorWrapper? = null

    init {
        val provider: AppInfoProvider = ManifestAppInfoProvider(context)
        val deviceUuid: UUID = UUID.randomUUID()
        tokenStore = SharedPreferencesTokenStore(context)
        tokenStore.load()
        tokenStore.autoPersist = true
        accountHelper = AndroidHelper.accountHelper(provider, deviceUuid, tokenStore)
        accountHelper.onSwitch { redditClient ->
            val logAdapter: LogAdapter = SimpleAndroidLogAdapter(Log.INFO)
            redditClient.logger = SimpleHttpLogger(SimpleHttpLogger.DEFAULT_LINE_LENGTH, logAdapter)
        }
    }

    val loggedIn: Boolean
    get() {
        return accountHelper.isAuthenticated()
    }

    fun loginUrl(): Flow<String> {
        val helper = accountHelper.switchToNewUser()
        val url = helper.getAuthorizationUrl(true, true, "read", "identity")
        this.helper = helper
        return flowOf(url)
    }

    fun checkUrl(url: String): Flow<Throwable?> {
        return if (helper?.isFinalRedirectUrl(url) == true) {
            flow {
                try {
                    helper!!.onUserChallenge(url)
                    emit(null)
                    helper = null
                } catch(t: Throwable) {
                    emit(t)
                }
            }.flowOn(Dispatchers.IO)
        } else {
            emptyFlow()
        }
    }

    fun moreSubredditPosts(subreddit: String, restart: Boolean = false): Flow<List<Submission>> {
        if (restart || subredditPaginatorWrapper?.subreddit != subreddit) {
            val subredditPaginator = accountHelper.reddit.subreddit(subreddit).posts()
                .sorting(SubredditSort.TOP)
                .timePeriod(TimePeriod.DAY)
                .limit(30)
                .build()
            subredditPaginatorWrapper = SubredditPaginatorWrapper(subreddit, subredditPaginator)
        }

        return flow {
            emit(subredditPaginatorWrapper!!.paginator.next())
        }.flowOn(Dispatchers.IO)
    }

    fun moreTrendingPosts(restart: Boolean = false): Flow<List<Submission>> {
        if (restart || homePaginator == null) {
            homePaginator = accountHelper.reddit.frontPage()
                .sorting(SubredditSort.TOP)
                .timePeriod(TimePeriod.DAY)
                .limit(30)
                .build()
        }

        return flow {
            emit(homePaginator!!.next())
        }.flowOn(Dispatchers.IO)
    }

    fun loadPost(id : String): Flow<Post> {
        return flow {
            val submission = accountHelper.reddit.submission(id)
            val comments = parse(submission.comments())
            emit(Post(submission.inspect(), comments))
        }.flowOn(Dispatchers.IO)
    }

    fun search(searchTerm: String): Flow<List<String>> {
        return flow {
            emit(accountHelper.reddit.searchSubredditsByName(searchTerm).map {
                it.name
            })
        }.flowOn(Dispatchers.IO)
    }

    data class SubredditPaginatorWrapper(
        val subreddit: String,
        val paginator: DefaultPaginator<Submission>
    )
}

// The walkTree() method from Android JRAW seems to have a bug, so we use our own.
private fun parse(root: RootCommentNode): List<PostComment> {
    return root.map {
        traverse(it)
    }.flatten()
}

private fun traverse(node: CommentNode<*>): List<PostComment> {
    val comments = mutableListOf<PostComment>()
    node.subject.body?.let { body ->
        comments.add(PostComment(
            body,
            node.depth,
            node.subject.author,
            node.subject.edited ?: node.subject.created
        ))
    }
    node.forEach {
        comments.addAll(traverse(it))
    }
    node.replies.forEach {
        comments.addAll(traverse(it))
    }

    return comments
}