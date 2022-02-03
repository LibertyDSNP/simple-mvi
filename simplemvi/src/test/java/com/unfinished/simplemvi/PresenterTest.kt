package com.unfinished.simplemvi

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.withFragment
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.IllegalArgumentException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PresenterTest {

    @Test
    fun source_empty() {
        val presenter = TestPresenter()

        runBlocking {
            assertThat(presenter.source().toList()).isEmpty()
        }
    }

    @Test
    fun createStateFlow() {
        val startingState = mockk<State>()
        val presenter = TestPresenter(startingState)
        launchFragment<TestRenderer>().withFragment {
            presenter.register(this)
            assertThat(this.renderedState).isSameAs(startingState)
            assertThat(presenter.lastState).isNull()
            assertThat(presenter.lastAction).isNull()
            assertThat(presenter.stateToReduce).isNull()
            assertThat(presenter.actionToReduce).isNull()
        }
    }

    @Test
    fun trigger() {
        val startingState = mockk<State>()
        val presenter = TestPresenter(startingState)
        launchFragment<TestRenderer>().withFragment {
            presenter.register(this)
            assertThat(this.renderedState).isSameAs(startingState)
            assertThat(presenter.stateToReduce).isNull()
            assertThat(presenter.actionToReduce).isNull()

            val action = mockk<Action>()
            val reducedState = mockk<State>()
            presenter.reducedState = reducedState
            presenter.trigger(action)
            assertThat(presenter.stateToReduce).isSameAs(startingState)
            assertThat(presenter.actionToReduce).isSameAs(action)
            assertThat(this.renderedState).isSameAs(reducedState)
        }
    }

    @Test
    fun collect() {
        val startingState = mockk<State>()
        val presenter = TestPresenter(startingState)
        launchFragment<TestRenderer>().withFragment {
            presenter.register(this)
            assertThat(this.renderedState).isSameAs(startingState)
            assertThat(presenter.stateToReduce).isNull()
            assertThat(presenter.actionToReduce).isNull()

            val action = mockk<Action>()
            val reducedState = mockk<State>()
            presenter.reducedState = reducedState

            presenter.callCollect(flowOf(action))

            assertThat(presenter.lastState).isSameAs(reducedState)
            assertThat(presenter.lastAction).isSameAs(action)
            assertThat(presenter.stateToReduce).isSameAs(startingState)
            assertThat(presenter.actionToReduce).isSameAs(action)
            assertThat(this.renderedState).isSameAs(reducedState)
        }
    }
}

class TestPresenter(private val startingState: State? = null): Presenter<State, Action>() {
    var stateToReduce: State? = null
    var actionToReduce: Action?  = null
    var reducedState: State? = null
    override fun startingState(): State = startingState ?: object : State {}
    override fun reduce(lastState: State, action: Action): State {
        stateToReduce = lastState
        actionToReduce = action
        return reducedState ?: lastState
    }
    fun callCollect(flow: Flow<Action>) {
        collect(flow)
    }
}

class TestRenderer: Fragment(), Renderer<State> {
    var renderedState: State? = null

    override fun render(state: State) {
        renderedState = state
    }
}