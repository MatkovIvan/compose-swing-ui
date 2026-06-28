package org.jetbrains.compose.swing.window

import androidx.compose.runtime.Applier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.awt.Frame
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioural tests asserting that [Window] arguments are reactive: mutating Compose state that
 * feeds a [Window] argument is reflected on the realized [JFrame] once the change is applied.
 */
class WindowReactivityTest {
    @Test
    fun titleReactsToRecomposition() = windowTest("title-test") { state ->
        val frame = frame()
        assertEquals(state.initialTitle, frame.title)
        state.title.value = "title-test-updated"
        awaitChange()
        assertEquals("title-test-updated", frame.title)
    }

    @Test
    fun visibleReactsToRecomposition() = windowTest("visible-test", initialVisible = false) { state ->
        val frame = frame()
        assertTrue(!frame.isVisible)
        state.visible.value = true
        awaitChange()
        assertTrue(frame.isVisible)
    }

    @Test
    fun resizableReactsToRecomposition() = windowTest("resizable-test") { state ->
        val frame = frame()
        assertTrue(frame.isResizable)
        state.resizable.value = false
        awaitChange()
        assertTrue(!frame.isResizable)
    }
}

private const val AWAIT_TIMEOUT_MILLIS = 15_000L

/** Compose state backing the reactive [Window] arguments under test. */
private class WindowState(
    val initialTitle: String,
    val title: MutableState<String>,
    val visible: MutableState<Boolean>,
    val resizable: MutableState<Boolean>,
)

/**
 * Drives a real [Window] through a controlled composition and lets [body] mutate [WindowState] and
 * observe the realized [JFrame]. The whole test runs on the Swing event dispatch thread; frames
 * are pumped deterministically via [WindowTestScope.awaitChange]. Skipped in headless environments
 * where no real frame can be realized.
 */
private fun windowTest(
    initialTitle: String,
    initialVisible: Boolean = true,
    body: suspend WindowTestScope.(WindowState) -> Unit,
) {
    if (GraphicsEnvironment.isHeadless()) return

    val state =
        WindowState(
            initialTitle = initialTitle,
            title = mutableStateOf(initialTitle),
            visible = mutableStateOf(initialVisible),
            resizable = mutableStateOf(true),
        )
    runBlocking(Dispatchers.Swing) {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        // The "application" composition driven by the injected recomposer/clock, mirroring
        // awaitApplication's wiring; Window mounts its content into its own JFrame via an interop
        // host, so the application composition has no Swing root of its own.
        val composition = Composition(NoOpApplier(), recomposer)
        val runner: Job = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent {
                Window(
                    onCloseRequest = {},
                    title = state.title.value,
                    visible = state.visible.value,
                    resizable = state.resizable.value,
                ) {}
            }
            val scope = WindowTestScope(state, clock, recomposer)
            scope.awaitChange()
            withTimeout(AWAIT_TIMEOUT_MILLIS) { scope.body(state) }
        } finally {
            composition.dispose()
            frameTitled(state.title.value)?.dispose()
            frameTitled(state.initialTitle)?.dispose()
            recomposer.close()
            runner.cancel()
        }
    }
}

private class WindowTestScope(
    private val state: WindowState,
    private val clock: BroadcastFrameClock,
    private val recomposer: Recomposer,
) {
    /** Returns the realized frame created by [Window], failing if it was never created. */
    fun frame(): JFrame = requireNotNull(frameTitled(state.title.value) ?: frameTitled(state.initialTitle)) {
        "Window did not realize a JFrame"
    }

    /** Flushes pending snapshot writes and pumps recomposition frames until the work drains. */
    suspend fun awaitChange() {
        Snapshot.sendApplyNotifications()
        while (recomposer.hasPendingWork) {
            clock.sendFrame(System.nanoTime())
            yield()
        }
        yield()
    }
}

private fun frameTitled(title: String): JFrame? =
    Frame.getFrames().filterIsInstance<JFrame>().firstOrNull { it.title == title }

/** A no-op applier, matching how the application composition has no Swing root of its own. */
private class NoOpApplier : Applier<Any> {
    override val current: Any = Unit

    override fun down(node: Any) = Unit

    override fun up() = Unit

    override fun insertTopDown(
        index: Int,
        instance: Any,
    ) = Unit

    override fun insertBottomUp(
        index: Int,
        instance: Any,
    ) = Unit

    override fun remove(
        index: Int,
        count: Int,
    ) = Unit

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) = Unit

    override fun clear() = Unit
}
