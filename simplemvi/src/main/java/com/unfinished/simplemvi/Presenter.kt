package com.unfinished.simplemvi

import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Interface that represents an MVI Model. You should use data classes if possible to implement this interface. The reason is that the equals() method of your State will be used if it is the same as the last one. If it is the same, it will not be forwarded to [Renderer.render].
 *
 */
interface State

/**
 * Interface that represents an MVI Intent. The best approach is to use a sealed class as your Action. Actions can be triggered by [Presenter.trigger], [Presenter.source], or [Presenter.collect].
 *
 */
interface Action

/**
 * Interface that represents an MVI View. Can be an Activity, Fragment, or View.
 * IMPORTANT: A view has to implement [LifecycleOwner]. Fragment and ComponentActivity do so by default. For custom views you have to implement the LifecycleOwner interface manually. You can do so with the following code snippet:
 *
 * ```
 * override fun getLifecycle(): Lifecycle {
 *    val lifecycleOwner = findViewTreeLifecycleOwner()
 *    requireNotNull(lifecycleOwner)
 *    return lifecycleOwner.lifecycle
 * }
 * ```
 *
 * The easiest way to register the [Renderer] in a custom view is by calling:
 *
 * ```
 * doOnAttach {
 *    presenter.register(this)
 * }
 * ```
 * in your view initialization (init).
 *
 * @param S your [State] class.
 */
interface Renderer<S: State>: LifecycleOwner {
    /**
     * Is responsible for rendering the latest [State] unto the UI.
     * This method is called by the [Presenter].
     *
     * @param state The latest state that will be rendered.
     */
    fun render(state: S)
}

/**
 * The presenter is responsible for processing [Action]s and reducing [Action]s and the last [State], to get the new [State].
 *
 * You MUST implement the following methods:
 * * startingState() - This method returns the initial state. It will be used to render the UI for the first time before any actions are triggered
 * * reduce() - This method is responsible for state reduction, i.e. combining the latest state with an incoming action and outputting the new state
 *
 * The following methods are optional to implement:
 * * source() - This method can be implemented to return a flow of actions from a data source (e.g. REST calls, database updates).
 * * process() - This method can be used to process incoming actions and trigger behavior based on that.
 *
 * @param S your [State] class
 * @param A your [Action] class
 */
abstract class Presenter<S: State, A: Action>: ViewModel() {
    private val actionFlow = MutableSharedFlow<A>(1)
    private var lifecycleOwner: LifecycleOwner? = null

    /**
     * Gives access to the last processed [Action].
     */
    var lastAction: Action? = null
    private set

    private val state: LiveData<S> by lazy {
        createStateFlow().asLiveData()
    }

    /**
     * Gives access to the last [State] as it was rendered.
     */
    var lastState: S? = null
    private set

    /**
     * This method can be implemented to return a flow of [Action]s from a data source (e.g. REST calls, database updates).
     * [Action]s emitted by this flow will be processed the same way [Action]s received via [Presenter.trigger] are being processed.
     *
     * @return A [Flow] of [Action]s that will be processed by the [Presenter].
     */
    open fun source(): Flow<A> = emptyFlow()

    /**
     * This method processes incoming [Action]s in the following way:
     *
     * 1. Receive [Action] from [Presenter.trigger], [Presenter.source], or [Presenter.collect]
     * 1. Hand [Action] to [process]
     * 2. Hand [Action] and last [State] to [reduce]
     * 3. [State] returned from [reduce] is passed to a [LiveData] instance. [LiveData] is used to handle the Android Activity/Fragment lifecycles
     * 4. When the [LiveData] instance receives a new [State], [Renderer.render] is called with the new [State]
     *
     * You can override this method if you want to customize the processing of [Action]s.
     *
     * @return A Flow of [State]s.
     */
    @ExperimentalCoroutinesApi
    open fun createStateFlow(): Flow<S> {
        return merge(actionFlow, source())
            .onEach {
                process(it)
            }.scan(startingState()) { state: S, action: A ->
                lastAction = action
                reduce(state, action).apply {
                    lastState = this
                }
            }.distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.Eagerly, 1)
    }

    /**
     * The method can be implemented to processing incoming [Action]s and trigger behavior based on them.
     *
     * @param action An [Action] that will be processed. [Presenter.reduce] will not be called until [Presenter.process] finishes.
     */
    protected open fun process(action: A) {}

    /**
     * Must be implemented to return the initial [State] that will be rendered by [Renderer.render].
     *
     * @return The initial [State]
     */
    abstract fun startingState(): S

    /**
     * Must be implemented to reduce an [Action] with the latest [State] to get the new [State].
     *
     * @param lastState The previous state as returned by [Presenter.reduce] (or [Presenter.startingState] the very first time).
     * @param action The [Action] that will be combined with the last [State] to get the new [State].
     * @return
     */
    abstract fun reduce(lastState: S, action: A): S

    /**
     * Can be called from the view (i.e. [Renderer]) to trigger processing of an [Action] by the [Presenter].
     *
     * @param action The [Action] that should be processed by the [Presenter].
     */
    fun trigger(action: A) {
        viewModelScope.launch {
            actionFlow.emit(action)
        }
    }

    /**
     * MUST be called to register the [Renderer] with the [Presenter].
     *
     * @param renderer The [Renderer] that will be used to render [State]s.
     */
    fun register(renderer: Renderer<S>) {
        this.lifecycleOwner = renderer
        state.observe(renderer, Observer {
            renderer.render(it)
        })
    }

    /**
     * Can be called from [Presenter.process] to queue up [Action]s to be processed by the presenter.
     * For example, you can use this method to make a REST call, convert the result into an action, and then recieve it in [Presenter.reduce].
     *
     * @param flow The [Flow] that will be collected by the presenter to receive [Action]s.
     */
    protected fun collect(flow: Flow<A>) {
        viewModelScope.launch {
            flow.collect {
                actionFlow.emit(it)
            }
        }
    }
}