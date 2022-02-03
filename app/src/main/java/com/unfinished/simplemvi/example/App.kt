package com.unfinished.simplemvi.example

import android.app.Application
import com.unfinished.simplemvi.example.repository.RedditRepository
import com.unfinished.simplemvi.example.ui.home.SearchPresenter
import com.unfinished.simplemvi.example.ui.login.LoginPresenter
import com.unfinished.simplemvi.example.ui.post.PostPresenter
import com.unfinished.simplemvi.example.ui.postlist.PostListPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.*


class App: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
        }

        val appModule = module {
            single { RedditRepository(this@App) }
        }

        val presenterModule = module {
            viewModel { LoginPresenter(get()) }
            viewModel { PostListPresenter(get()) }
            viewModel { PostPresenter(get()) }
            viewModel { SearchPresenter(get()) }
        }

        loadKoinModules(listOf(appModule, presenterModule))
    }
}