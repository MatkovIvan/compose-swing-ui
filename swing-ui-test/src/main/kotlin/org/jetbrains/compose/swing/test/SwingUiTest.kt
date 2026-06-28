package org.jetbrains.compose.swing.test

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteraction
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteractionCollection
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import javax.swing.JPanel

/**
 * The user-facing handle for driving a single isolated Swing-Compose composition under test.
 *
 * An instance is created by [runSwingUiTest] and is only valid for the duration of the supplied
 * test block. The test body runs **on the AWT event dispatch thread (EDT)**, so every query, action
 * and assertion reads and writes the real AWT component tree directly, with no thread hop:
 *
 * ```
 * @Test
 * fun clickingTheButtonUpdatesTheLabel() = runSwingUiTest {
 *     var clicks by mutableStateOf(0)
 *     setContent {
 *         Button(text = "Clicks: $clicks", onClick = { clicks++ })
 *     }
 *     onNodeWithText("Clicks: 0").performClick()
 *     onNodeWithText("Clicks: 1").assertExists()
 * }
 * ```
 *
 * The composition runs on [Dispatchers.Swing] (the EDT) with frames produced under test control;
 * frames are never produced automatically.
 */
public interface SwingUiTest {
    /**
     * The root [Container] hosting the composition. Useful for advanced assertions, e.g. inspecting
     * a child's layout constraint via the parent's [java.awt.LayoutManager].
     */
    public val root: Container

    /**
     * Sets the composable [content] of the test [root] and settles the composition so the AWT tree
     * reflects the initial state before returning. May be called only once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    public fun setContent(content: @Composable () -> Unit)

    /**
     * Suspends until the composition is idle, making the AWT tree reflect the latest state.
     *
     * This is suspending rather than blocking, so recomposition can make progress while it waits. It
     * returns once there is neither pending recomposition nor pending snapshot work.
     *
     * If the composition never settles within a generous frame cap, this fails with an
     * [AssertionError] whose message names the outstanding work and includes a readable dump of the
     * current AWT tree, rather than hanging until the surrounding test framework times out.
     */
    public suspend fun awaitIdle()

    /**
     * Suspends until [condition] returns `true`, driving frames between checks.
     *
     * Prefer [awaitIdle] followed by a plain assertion wherever it suffices: it is fully
     * deterministic. Use [waitUntil] only when a settled condition cannot be expressed that way
     * (e.g. work gated on genuinely external timing).
     *
     * Bounded by BOTH a frame (iteration) cap and the [timeoutMillis] wall-clock deadline; whichever
     * trips first fails with an [AssertionError] that includes a tree dump. The frame cap keeps CI
     * deterministic: a condition that never becomes true fails after a fixed number of frames
     * regardless of machine speed.
     *
     * @param timeoutMillis the wall-clock deadline after which an unmet condition fails the test.
     * @param condition the predicate to await; evaluated on the EDT.
     */
    public suspend fun waitUntil(
        timeoutMillis: Long = 1_000,
        condition: () -> Boolean,
    )

    /**
     * Finds the single node whose text equals [text] (or contains it when [substring] is `true`).
     * The match is resolved lazily when the returned interaction is first used.
     */
    public fun onNodeWithText(
        text: String,
        substring: Boolean = false,
    ): SwingNodeInteraction

    /**
     * Finds the single node whose [Component.getName] equals [name].
     */
    public fun onNodeWithName(name: String): SwingNodeInteraction

    /**
     * Finds the single node tagged with [tag] via `SwingModifier.testTag`.
     */
    public fun onNodeWithTag(tag: String): SwingNodeInteraction

    /**
     * Finds the single node matching [matcher]. The match is resolved lazily when the returned
     * interaction is first used, and resolution fails if zero or more than one node matches.
     */
    public fun onNode(matcher: SwingMatcher): SwingNodeInteraction

    /**
     * Finds all nodes whose text equals [text] (or contains it when [substring] is `true`).
     */
    public fun onAllNodesWithText(
        text: String,
        substring: Boolean = false,
    ): SwingNodeInteractionCollection

    /**
     * Finds all nodes tagged with [tag] via `SwingModifier.testTag`.
     */
    public fun onAllNodesWithTag(tag: String): SwingNodeInteractionCollection

    /**
     * Finds all nodes matching [matcher].
     */
    public fun onAllNodes(matcher: SwingMatcher): SwingNodeInteractionCollection

    /**
     * Returns an interaction targeting the composition [root] itself.
     */
    public fun onRoot(): SwingNodeInteraction
}

/**
 * Finds the single node of type [T]. Convenience for `onNode(SwingMatcher.isOfType<T>())`.
 */
public inline fun <reified T : Component> SwingUiTest.onNodeOfType(): SwingNodeInteraction =
    onNode(SwingMatcher.isOfType<T>())

/**
 * Finds all nodes of type [T]. Convenience for `onAllNodes(SwingMatcher.isOfType<T>())`.
 */
public inline fun <reified T : Component> SwingUiTest.onAllNodesOfType(): SwingNodeInteractionCollection =
    onAllNodes(SwingMatcher.isOfType<T>())

/**
 * Sets up an isolated Swing-Compose composition, runs the suspending [block] against it on the EDT,
 * and tears everything down.
 *
 * The whole [block] executes as a coroutine on [Dispatchers.Swing] (the EDT); the calling (JUnit)
 * thread is blocked until it completes. Frames are produced under test control rather than
 * automatically, so the composition advances only across idle/await calls. Because the body runs on
 * the EDT, queries and actions read and write the AWT tree directly, and [SwingUiTest.awaitIdle]
 * suspends rather than blocking.
 *
 * Runs fully under `-Djava.awt.headless=true`: the root is given a fixed size and laid out
 * synchronously on every idle pass, so tree/text/constraint AND bounds-based assertions (see
 * [SwingNodeInteraction.assertIsDisplayed]) all work without realizing an on-screen window.
 */
public fun runSwingUiTest(block: suspend SwingUiTest.() -> Unit) {
    // Host the test coroutine on the EDT and block the calling thread until it finishes. The whole
    // body therefore runs on the EDT, alongside the recomposer the impl launches on Dispatchers.Swing.
    runBlocking(Dispatchers.Swing) {
        val impl = SwingUiTestImpl()
        try {
            impl.block()
        } finally {
            impl.dispose()
        }
    }
}

private class SwingUiTestImpl : SwingUiTest {
    override val root: Container =
        JPanel().apply {
            // Give the off-screen root a concrete size so a forced layout pass assigns real,
            // non-zero bounds to descendants. Without this, an unrealized container reports zero
            // size and every child lays out to 0x0, making bounds-based assertions meaningless.
            // We never attach the root to a Window, so no peer is realized and no UI is shown.
            size = Dimension(ROOT_WIDTH, ROOT_HEIGHT)
            preferredSize = Dimension(ROOT_WIDTH, ROOT_HEIGHT)
        }

    private val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Swing + Job() + clock)
    private val recomposer = Recomposer(scope.coroutineContext)

    private var disposeHandle: DisposableHandle? = null
    private var contentSet = false
    private var frameTimeNanos = 0L

    init {
        scope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    override fun setContent(content: @Composable () -> Unit) {
        check(!contentSet) { "setContent may only be called once per test." }
        contentSet = true
        // We are already on the EDT (the whole test body runs there), so mount synchronously.
        disposeHandle = root.setContent(recomposer = recomposer, content = content)
        // Drive an initial settle so the AWT tree reflects the initial state before returning. We are
        // on the EDT and cannot suspend here, so we pump frames and let the recomposer (queued on the
        // EDT) make progress by spinning a nested AWT secondary loop between frames.
        settleBlocking()
    }

    override suspend fun awaitIdle() {
        // Deterministic idle gate. Because the test body runs on the EDT, a blocking wait would
        // deadlock the recomposer (which also runs on the EDT); instead we suspend, yielding the EDT
        // back to the recomposer between frames so it can recompose and apply changes.
        //
        // Each iteration: publish pending snapshot writes, send exactly one frame, yield the EDT so
        // the recomposer coroutine runs, then force a synchronous layout pass. Stop once neither the
        // recomposer nor the snapshot system has outstanding work.
        var iterations = 0
        while (true) {
            // Deliver pending snapshot writes to the recomposer ourselves rather than relying on the
            // production GlobalSnapshotManager, whose apply-notification dispatch is asynchronous and
            // backed by process-wide mutable dedup state shared across tests. Calling it directly here
            // makes each test's idle gate self-contained and deterministic.
            Snapshot.sendApplyNotifications()
            // A frame may be requested either by the recomposer (pending invalidations) or by an
            // effect awaiting withFrameNanos; send one unconditionally so awaiters proceed.
            frameTimeNanos += FRAME_INTERVAL_NANOS
            clock.sendFrame(frameTimeNanos)
            // Hand the EDT to the recomposer (also on Dispatchers.Swing) so it can observe the frame,
            // recompose, and apply changes to the AWT tree before we re-check idleness.
            yield()
            // Force a synchronous layout pass so descendants get real, non-zero bounds off-screen.
            layoutRoot()
            if (!recomposer.hasPendingWork && !Snapshot.current.hasPendingChanges()) {
                return
            }
            if (++iterations >= MAX_IDLE_FRAMES) {
                throw AssertionError(
                    "awaitIdle did not settle after $MAX_IDLE_FRAMES frames: there is still " +
                        "pending recomposition work or pending snapshot changes " +
                        "(hasPendingWork=${recomposer.hasPendingWork}, " +
                        "hasPendingChanges=${Snapshot.current.hasPendingChanges()}). " +
                        "The composition likely never reaches a stable frame. Current tree:\n" +
                        root.dumpTree(),
                )
            }
        }
    }

    override suspend fun waitUntil(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ) {
        // Escape hatch (see interface KDoc). Bounded by BOTH an iteration cap (frames pumped) and a
        // wall-clock deadline; whichever trips first fails. The iteration cap is the deterministic
        // bound that keeps CI from stalling when a condition never becomes true regardless of how
        // fast the machine is; the wall-clock deadline is a secondary guard for conditions gated on
        // genuinely external timing.
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        var iterations = 0
        while (true) {
            if (condition()) return
            if (iterations >= MAX_WAIT_UNTIL_FRAMES || System.nanoTime() >= deadline) {
                throw AssertionError(
                    "Condition still not met after $iterations frames / ${timeoutMillis}ms. " +
                        "Current tree:\n" + root.dumpTree(),
                )
            }
            Snapshot.sendApplyNotifications()
            frameTimeNanos += FRAME_INTERVAL_NANOS
            clock.sendFrame(frameTimeNanos)
            yield()
            layoutRoot()
            iterations++
        }
    }

    /**
     * Blocking variant of the idle gate used only by [setContent]'s one-shot initial settle. It pumps
     * frames and spins the EDT message queue inline (the recomposer runs as queued EDT tasks) without
     * suspending. Safe because it is called exactly once, before the test body needs the EDT for
     * anything else.
     */
    private fun settleBlocking() {
        var iterations = 0
        while (true) {
            Snapshot.sendApplyNotifications()
            frameTimeNanos += FRAME_INTERVAL_NANOS
            clock.sendFrame(frameTimeNanos)
            pumpEdtQueue()
            layoutRoot()
            if (!recomposer.hasPendingWork && !Snapshot.current.hasPendingChanges()) {
                return
            }
            if (++iterations >= MAX_IDLE_FRAMES) {
                throw AssertionError(
                    "setContent did not settle after $MAX_IDLE_FRAMES frames " +
                        "(hasPendingWork=${recomposer.hasPendingWork}, " +
                        "hasPendingChanges=${Snapshot.current.hasPendingChanges()}). Current tree:\n" +
                        root.dumpTree(),
                )
            }
        }
    }

    /**
     * Lets Runnables already queued on the EDT (including the recomposer's apply step) run, without
     * leaving the EDT. We are on the EDT, so we cannot block on `invokeAndWait`; instead we enter an
     * AWT [java.awt.SecondaryLoop] and post a task that exits it. The secondary loop processes the
     * pending events first, so the recomposer's continuation runs before this returns.
     */
    private fun pumpEdtQueue() {
        val loop =
            java.awt.Toolkit
                .getDefaultToolkit()
                .systemEventQueue
                .createSecondaryLoop()
        // Post the exit AFTER the recomposer's already-queued continuation, so the loop drains those
        // first. enter() blocks the current EDT dispatch until exit() runs, while still pumping events.
        javax.swing.SwingUtilities.invokeLater { loop.exit() }
        loop.enter()
    }

    private fun layoutRoot() {
        // Re-assert the root size (so it never collapses) and run a synchronous layout pass so every
        // descendant receives real bounds. The applier only calls revalidate(), which defers layout
        // to the RepaintManager; with no realized peer that deferred pass may never run, leaving
        // children at 0x0.
        //
        // We cannot use validate(): on a container with no native peer / validate-root it
        // short-circuits and assigns no child bounds. We instead drive doLayout() top-down ourselves —
        // each container is sized by its parent's layout before we lay out its own children — which
        // assigns real bounds throughout the tree synchronously on the EDT.
        root.setSize(ROOT_WIDTH, ROOT_HEIGHT)
        layoutTree(root)
    }

    private fun layoutTree(component: Component) {
        if (component is Container) {
            // Lay out this container first so its children receive their bounds, then recurse so each
            // child (now sized) lays out its own descendants.
            component.doLayout()
            for (child in component.components) layoutTree(child)
        }
    }

    override fun onNodeWithText(
        text: String,
        substring: Boolean,
    ): SwingNodeInteraction = onNode(SwingMatcher.hasText(text, substring))

    override fun onNodeWithName(name: String): SwingNodeInteraction = onNode(SwingMatcher.hasName(name))

    override fun onNodeWithTag(tag: String): SwingNodeInteraction = onNode(SwingMatcher.hasTestTag(tag))

    override fun onNode(matcher: SwingMatcher): SwingNodeInteraction =
        SwingNodeInteraction(this, matcher, description = matcher.description)

    override fun onAllNodesWithText(
        text: String,
        substring: Boolean,
    ): SwingNodeInteractionCollection = onAllNodes(SwingMatcher.hasText(text, substring))

    override fun onAllNodesWithTag(tag: String): SwingNodeInteractionCollection =
        onAllNodes(SwingMatcher.hasTestTag(tag))

    override fun onAllNodes(matcher: SwingMatcher): SwingNodeInteractionCollection =
        SwingNodeInteractionCollection(this, matcher)

    override fun onRoot(): SwingNodeInteraction =
        SwingNodeInteraction(this, SwingMatcher.isRoot(root), description = "root")

    fun dispose() {
        disposeHandle?.dispose()
        recomposer.cancel()
        scope.cancel()
    }

    private companion object {
        // 60fps cadence; only the monotonic progression matters for frame-driven effects.
        const val FRAME_INTERVAL_NANOS: Long = 16_666_667L

        // Off-screen root dimensions. Large enough that realistic test layouts get sensible,
        // non-zero child bounds under a forced layout pass.
        const val ROOT_WIDTH: Int = 800
        const val ROOT_HEIGHT: Int = 600

        // Generous frame caps. A healthy composition settles in a handful of frames; these bounds
        // exist only to convert a pathological never-settling loop into a readable failure instead
        // of an indefinite hang.
        const val MAX_IDLE_FRAMES: Int = 10_000
        const val MAX_WAIT_UNTIL_FRAMES: Int = 10_000
    }
}
