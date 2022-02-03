package com.unfinished.simplemvi.example.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.unfinished.simplemvi.Renderer
import com.unfinished.simplemvi.example.R
import com.unfinished.simplemvi.example.databinding.ActivityLoginBinding
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * This shows how to use Simple MVI with an [Activity].
 * The Activity has to implement the [Renderer] interface.
 * All you need to do is to implement the [render] method and call [com.unfinished.simplemvi.Presenter.register].
 *
 * If you want to trigger [com.unfinished.simplemvi.Action]s (i.e. MVI intents), call [com.unfinished.simplemvi.Presenter.trigger].
 *
 */
class LoginActivity: AppCompatActivity(), Renderer<LoginState> {
    lateinit var binding: ActivityLoginBinding
    val presenter: LoginPresenter by viewModel()

    companion object {
        fun createIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.login)

        binding.webView.clearCache(true)
        binding.webView.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                // This is how you can trigger the CheckUrl action, which will be processed by the Presenter and lead to an updated [State]. This updated [State] is then passed into the render() method.
                presenter.trigger(LoginAction.CheckUrl(url))
            }
        }

        presenter.register(this)
        // When the activity starts, we want to trigger the login process. Since Simple MVI is lifecycle aware, the processing won't start until the activity is started.
        // Alternatively to this line (and handling StartLogin in LoginPresenter.process, LoginPresenter could override source() like this:
        //        override fun source(): Flow<LoginAction> = repository.loginUrl().map { url ->
        //            LoginAction.LoginUrlReceived(url)
        //        }
        presenter.trigger(LoginAction.StartLogin)
    }

    override fun render(state: LoginState) {
        // [State] cannot only be used to render UI, but also to indicate that the Activity is not longer needed and can be closed.
        if (state.loginSuccess) {
            setResult(Activity.RESULT_OK)
            finish()
        }
        state.loginUrl?.let {
            if (binding.webView.url != it) {
                binding.webView.loadUrl(it)
            }
        }
    }
}