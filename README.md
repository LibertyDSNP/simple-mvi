Simple MVI
====================

Simple MVI is an Android library that supports the [Model-View-Intent (MVI)][1] architecture pattern.

It does so using [Kotlin Coroutine Flows][2]. If you prefer RxJava instead, you can still use the Simple MVI library by bridging Kotlin Coroutine Flows to RxJava, for example with [this library][3].

**This library is lifecycle aware (see [Activity lifecycle][4], [Fragment lifecycle][5]) by using [LiveData][6].** This means that you don't need to handle things like unsubscribing from your REST calls when a Fragment is paused/stopped by yourself.

If you want to learn what MVI is and how it works, please see an introduction [here][7] or [here][8].
This library provides interfaces for the model, the view, and the intent and a presenter that ties it all together. It allows you to only focus on the actual implementation of your business logic without writing any boilerplate code. 

# Gradle Dependency
```gradle
repositories {
  mavenCentral()
}

dependencies {
    implementation 'com.unfinished.simplemvi:simplemvi:1.0.0'
}
```

# Quick Start

You need 4 parts to use this library:
* Model: implements interface **State**
* Intent: implements interface **Action**
* View: implements interface **Renderer**
* Presenter: processes intents, updates the model and triggers view updates

```kotlin
// --- The Model ---
data class HomeScreenState(
    val loading: Boolean = true,
    val someTextToShow: String? = null,
    val showSearchBar: Boolean = false
): State

// --- The Intent(s) ---
sealed class HomeScreenAction: Action {
    data class DataLoaded(val someData: Data): HomeScreenAction()
    object StartSearch: HomeScreenAction()
}

// --- The View ---
class HomeScreenFragment: Fragment(), Renderer<HomeScreenState> {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.searchButton).setOnClickListener {
            presenter.trigger(HomeScreenAction.StartSearch) // trigger an action to be processed by the presenter
        }
        
        presenter.register(this) // connect renderer to presenter
    }
    
    // renders the latest model (i.e. state) to the user, called by the presenter
    override fun render(state: HomeScreenState) {
        findViewById<TextView>(R.id.titleTextView).text = state.someTextToShow
        findViewById<ProgressBar>(R.id.progressBar).visibility = if (state.loading) {
            View.VISIBLE
        } else {
            View.GONE
        }
        findViewById<SearchBar>(R.id.searchBar).visibility = if (state.showSearchBar) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}

// --- The Presenter ---
class HomeScreenPresenter: Presenter<HomeScreenState, HomeScreenAction>() {
    override fun startingState() = HomeScreenState()

    // conducts a state reduction: Takes the action and the last state, to create the new state
    override fun reduce(lastState: HomeScreenState, action: HomeScreenAction) {
        return when (action) {
            is HomeScreenStateAction.DataLoaded -> lastState.copy(
                loading = false,
                someTextToShow = action.data.text
            )
            is HomeScreenStateAction.StartSearch -> lastState.copy(
                showSearchBar = true
            )
        }
    }
}
```

# Detailed Explanation

The following diagram shows how the different components of Simple MVI interact and how the Presenter is orchestrating everything:

![Simple MVI Flow Diagram][9]

1. The initial state is retrieved from Presenter.startingState(), which you need to implement in your presenter class.
2. The starting/new state will be passed to the Renderer.render() method. Here the UI will be rendered based on the latest state.
3. An action can be triggered by the user when interacting with the UI (i.e. Activity / Fragment / View). To trigger the processing of an action (i.e. a user input), call Presenter.trigger(Action) from your Activity, Fragment, or View.
4. *(OPTIONAL)* If you want to connect a data source, you should implement Presenter.source(). It allows you to do things like making an initial REST call to retrieve data to be rendered or to get a constant stream of updates from a real time data source like Firebase or MongoDB Realm.
5. *(OPTIONAL)* If you want to do things other than UI updates when an action has been triggered, you can override the Presenter.process() method and handle actions there. They will still be forwarded to Presenter.reduce() no matter what you do with the action inside of Presenter.process(), but only after the Presenter.process() method finishes.
6. *(OPTIONAL)* When you override Presenter.process() (see 5.) and handle actions there, you can use Presenter.collect() to collect the results of a Flow. Presenter.collect() takes a Flow of Action objects. Those actions will be processed like any other action.
7. The reduce method conducts a state reduction. It takes the latest state (i.e. model) and combines it with the action (i.e. intent). The result is the new state (i.e. model).

## Model (State)

The model class has to implement the interface State. It contains all the information that is necessary to render the UI.
It is important that the equals() method is properly implemented for this class. The easiest way to achieve that is by using [Kotlin data classes][10]. The reason is that equals() is used to determine if a state is the same as the previous one. If it is, it will not be passed to Renderer.render() (because nothing new would need to be rendered anyways).
The initial state is created by Presenter.startState(), which you need to override. Subsequent states are produced through state reduction by Presenter.reduce().

Example:
```kotlin
data class HomeScreenState(
    val loading: Boolean = true,
    val someTextToShow: String? = null,
    val showSearchBar: Boolean = false
): State
```

## Intent (Action)

Intents have to implement the Action interface. The best way to do that is by using a sealed class. The subclasses represent individual actions (i.e. intents) that can be triggered.
If an action has data attached to it, it's easiest to use data classes. If no data is attached to an action, Kotlin object expressions can be used.
Actions can be triggered in 3 different ways:
1. **Presenter.trigger()** - This is the most common use case. This method can be called with an action as a parameter when the user interacts with the UI.
2. **Presenter.source()** - This method can be implemented to return a flow of actions from a data source (e.g. REST calls, database updates).
3. **Presenter.collect()** - This method should only be called from within Presenter.process(). It can be used to request or process data and return the result in form of an action. For example, you could use this to refresh data from a REST endpoint when the user clicks on a refresh button in the UI.

Example:
```kotlin
sealed class HomeScreenAction: Action {
    data class DataLoaded(val someData: Data): HomeScreenAction()
    object StartSearch: HomeScreenAction()
    data class SearchForTerm(val searchTerm: String): HomeScreenAction()
}
```

## View (Renderer)

A view can be a Fragment, an Activity, or a View.

**IMPORTANT: A view has to implement [LifecycleOwner][11] (Renderer extends form LifecycleOwner).**
Fragments and Activities extending ComponentActivity do so by default. If you want to use a custom view, you have to implement LifefycleOwner yourself. This can be done very easily (see [example][12]).

**IMPORTANT: You have to call presenter.register(Renderer) so that the presenter can update the Renderer (i.e. view) when the state (i.e. model) changes.**

Example:
```kotlin
class HomeScreenFragment: Fragment(), Renderer<HomeScreenState> {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.searchButton).setOnClickListener {
            presenter.trigger(HomeScreenAction.StartSearch) // trigger an action to be processed by the presenter
        }
        
        presenter.newsFilter = arguments?.getString(NEWS_FILTER) // set a field on the presenter that will be used inside of Presenter.source() to retrieve data
        presenter.register(this) // register the renderer with the presenter, so that the presenter can call render() on the renderer
    }

    // renders the latest model (i.e. state) to the user, called by the presenter
    override fun render(state: HomeScreenState) {
        findViewById<TextView>(R.id.titleTextView).text = state.someTextToShow
        findViewById<ProgressBar>(R.id.progressBar).visibility = if (state.loading) {
            View.VISIBLE
        } else {
            View.GONE
        }
        findViewById<SearchBar>(R.id.searchBar).visibility = if (state.showSearchBar) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
```

## Presenter

The presenter is responsible for processing actions and reducing actions and the last state, to get the new state.

You **MUST** implement the following methods:
* startingState() - This method returns the initial state. It will be used to render the UI for the first time before any actions are triggered
* reduce() - This method is responsible for state reduction, i.e. combining the latest state with an incoming action and outputting the new state

The following methods are **optional** to implement:
* source() - This method can be implemented to return a flow of actions from a data source (e.g. REST calls, database updates).
* process() - This method can be used to process incoming actions and trigger behavior based on them.

Example:
```kotlin
class HomeScreenPresenter: Presenter<HomeScreenState, HomeScreenAction>() {
    lateinit var newsFilter: String?
    override fun startingState() = HomeScreenState()
    
    override fun source() = repository.loadNews(newsFilter).map {
        HomeScreenAction.DataLoaded(it)
    }

    override fun process(action: HomeScreenAction) {
        when (action) {
            is SearchForTerm -> collect(repository.search(action.searchTerm).map {
                DataLoaded(it)
            })
        }
    }

    // conducts a state reduction: Takes the action and the last state, to create the new state
    override fun reduce(lastState: HomeScreenState, action: HomeScreenAction) {
        return when (action) {
            is HomeScreenStateAction.DataLoaded -> lastState.copy(
                loading = false,
                someTextToShow = action.data.text
            )
            is HomeScreenStateAction.StartSearch -> lastState.copy(
                showSearchBar = true
            )
        }
    }
}
```

# Sample Code

Please check out the [sample app][13] to see in detail and with different examples, how Simple MVI can be used.

License
=======

        Copyright 2022 Unfinished Labs LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[1]: http://hannesdorfmann.com/android/model-view-intent/
[2]: https://kotlinlang.org/docs/flow.html
[3]: https://github.com/Kotlin/kotlinx.coroutines/tree/master/reactive/kotlinx-coroutines-rx2
[4]: https://developer.android.com/guide/components/activities/activity-lifecycle
[5]: https://developer.android.com/guide/fragments/lifecycle
[6]: https://developer.android.com/topic/libraries/architecture/livedata
[7]: https://www.raywenderlich.com/817602-mvi-architecture-for-android-tutorial-getting-started
[8]: https://medium.com/swlh/mvi-architecture-with-android-fcde123e3c4a
[9]: simplemvi_flow.svg
[10]: https://kotlinlang.org/docs/data-classes.html
[11]: https://developer.android.com/reference/androidx/lifecycle/LifecycleOwner
[12]: ./app/src/main/java/com/unfinished/simplemvi/example/ui/home/SearchView.kt
[13]: ./app/src/main/java/com/unfinished/simplemvi/example/