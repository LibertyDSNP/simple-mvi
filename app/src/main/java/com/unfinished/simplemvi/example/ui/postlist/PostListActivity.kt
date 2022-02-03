package com.unfinished.simplemvi.example.ui.postlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

private const val ARG_SUBREDDIT = "subreddit"

class PostListActivity: AppCompatActivity() {

    companion object {
        fun createIntent(context: Context, subreddit: String? = null) = Intent(context, PostListActivity::class.java).apply {
            putExtra(ARG_SUBREDDIT, subreddit)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(ARG_SUBREDDIT)

        supportFragmentManager.beginTransaction()
            .replace(
                android.R.id.content,
                PostListFragment.newInstance(intent.getStringExtra(ARG_SUBREDDIT))
            ).commit()
    }
}