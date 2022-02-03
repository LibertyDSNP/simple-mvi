package com.unfinished.simplemvi.example.ui.home

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.unfinished.simplemvi.Renderer
import com.unfinished.simplemvi.example.databinding.ViewSearchBinding
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent

/**
 * This shows how to use Simple MVI with a custom [android.view.View].
 * In order to register the custom [android.view.View] with the [com.unfinished.simplemvi.Presenter], the view needs to implement [androidx.lifecycle.LifecycleOwner].
 * This easiest way to register the view with the presenter, is to use kotlin's doOnAttach() method:
 * ```
 * doOnAttach {
      presenter.register(this)
 * }
 * ```
 *
 * In combination with using [findViewTreeLifecycleOwner] in [getLifecycle], this will ensure that when the presenter requests the lifecycle from the view, it is actually present.
 *
 */
class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), Renderer<SearchState>, KoinComponent, LifecycleOwner {
    private val binding = ViewSearchBinding.inflate(LayoutInflater.from(context), this)
    private val presenter: SearchPresenter by lazy {
        (context as ComponentActivity).getViewModel()
    }
    private val searchResults = mutableListOf<String>()
    var searchResultSelectedListener: ((searchResult: String) -> Unit)? = null

    init {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(editable: Editable) {
                presenter.trigger(SearchAction.SearchTermChanged(editable.toString()))
            }

        })
        binding.searchImageButton.setOnClickListener {
            presenter.trigger(SearchAction.Search)
        }
        binding.clearImageButton.setOnClickListener {
            presenter.trigger(SearchAction.Clear)
        }
        binding.recyclerView.adapter = SearchResultAdapter(searchResults) {
            searchResultSelectedListener?.invoke(it)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        // This ensures, that presenter.register() is only called, once the view is attached to a window and therefore, belongs to a LifecycleOwner (Activity, or Fragment). If registration is not postponed to that point in time, findViewTreeLifecycleOwner() will return null.
        doOnAttach {
            presenter.register(this)
        }
    }

    override fun render(state: SearchState) {
        if (binding.searchEditText.text.toString() != state.searchTerm) {
            binding.searchEditText.setText(state.searchTerm ?: "")
        }
        binding.clearImageButton.isEnabled = state.buttonsEnabled
        binding.searchImageButton.isEnabled = state.buttonsEnabled
        searchResults.clear()
        state.searchResults?.let {
            searchResults.addAll(it)
        }
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    /**
     * This method uses the LifecycleOwner of the Activity or Fragment this view is attached to, to retrieve the lifecycle.
     */
    override fun getLifecycle(): Lifecycle {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        requireNotNull(lifecycleOwner)
        return lifecycleOwner.lifecycle
    }
}