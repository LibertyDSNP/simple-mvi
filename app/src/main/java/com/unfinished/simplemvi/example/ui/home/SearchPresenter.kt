package com.unfinished.simplemvi.example.ui.home

import com.unfinished.simplemvi.Action
import com.unfinished.simplemvi.Presenter
import com.unfinished.simplemvi.State
import com.unfinished.simplemvi.example.repository.RedditRepository
import kotlinx.coroutines.flow.map

/**
 * Presenter for [SearchView].
 *
 * @property repository
 */
class SearchPresenter(
    private val repository: RedditRepository
): Presenter<SearchState, SearchAction>() {
    override fun startingState(): SearchState = SearchState()

    override fun process(action: SearchAction) {
        when (action) {
            // When the user triggers a search, we first need to get the search term (from the latest [State]) and then query the repository for search results.
            // The returned data has to be mapped to an [Action] so that it can be collected by the [Presenter].
            is SearchAction.Search -> lastState?.searchTerm?.let { searchTerm ->
                collect(repository.search(searchTerm).map { result ->
                    SearchAction.SearchResult(result)
                })
            }
        }
    }

    override fun reduce(lastState: SearchState, action: SearchAction): SearchState {
        return when (action) {
            // If the search term has changed, udpate it in the [State] and enable the buttons (only when there is a search term in the search fields does it make sense to activie the search and clear button)
            is SearchAction.SearchTermChanged -> lastState.copy(
                searchTerm = action.searchTerm,
                buttonsEnabled = action.searchTerm.isNotBlank()
            )
            // Clear the search term, results and disabled buttons
            is SearchAction.Clear -> lastState.copy(
                searchTerm = null,
                searchResults = null,
                buttonsEnabled = false
            )
            // When the search is triggered, we want to show a loading spinner
            is SearchAction.Search -> lastState.copy(
                loading = true
            )
            // The a search result is returned, we set it on the [State] so the UI can render it and we hide the loading spinner (as loading is complete).
            is SearchAction.SearchResult -> lastState.copy(
                searchResults = action.results,
                loading = false
            )
        }
    }
}

data class SearchState(
    val loading: Boolean = false,
    val searchTerm: String? = null,
    val searchResults: List<String>? = null,
    val buttonsEnabled: Boolean = false
) : State

sealed class SearchAction: Action {
    data class SearchTermChanged(val searchTerm: String): SearchAction()
    object Search: SearchAction()
    object Clear: SearchAction()
    data class SearchResult(val results: List<String>): SearchAction()
}