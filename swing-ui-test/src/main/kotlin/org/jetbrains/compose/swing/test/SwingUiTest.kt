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
import org.jetbrains.compose.swing.test.interaction.NodePick
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteraction
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteractionCollection
import org.jetbrains.compose.swing.test.interaction.SwingWindowInteraction
import org.jetbrains.compose.swing.test.interaction.SwingWindowInteractionCollection
import org.jetbrains.compose.swing.test.interaction.realizedWindowsTreeDump
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
     * returns once there is neither pending recomposition nor pending snapshot work AND the EDT
     * queue has drained the runnables the settled composition scheduled — a window show that a
     * `Dialog { }` defers to its own dispatch, for example, has landed by the time this returns.
     *
     * If the composition never settles within a generous frame cap, this fails with an
     * [AssertionError] whose message names the outstanding work and includes a readable dump of the
     * current AWT tree (including composition-owned windows), rather than hanging until the
     * surrounding test framework times out.
     */
    public suspend fun awaitIdle()

    /**
     * Suspends until [condition] returns `true`, driving frames between checks.
     *
     * Prefer [awaitIdle] followed by a plain assertion wherever it suffices: it is fully
     * deterministic. Use [waitUntil] only when a settled condition cannot be expressed that way
     * (e.g. work gated on genuinely external timing).
     *
     * Bounded by BOTH a frame cap and the [timeoutMillis] wall-clock deadline; whichever trips first
     * fails with an [AssertionError] that includes a tree dump. The frame cap counts only frames the
     * composition consumes, keeping CI deterministic: a condition gated on a recomposition or
     * frame-effect loop that never becomes true fails after a fixed number of frames regardless of
     * machine speed, while a condition gated on external timing (e.g. a native window-system event)
     * keeps being polled until the wall-clock deadline.
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

    /**
     * Finds the single window matching [matcher] realized by the composition — a top-level window
     * realized by a [org.jetbrains.compose.swing.window.Window] or
     * [org.jetbrains.compose.swing.window.Dialog] composable that is currently in the composition,
     * whether or not it is shown. A top-level window from any other source, or a disposed peer that
     * left the composition, is never matched. The match is resolved lazily when the returned
     * interaction is first used.
     *
     * ```
     * setContent { Window(onCloseRequest = {}, title = "Settings") { … } }
     * onWindow(SwingMatcher.hasTitle("Settings")).assertIsVisible()
     * ```
     */
    public fun onWindow(matcher: SwingMatcher): SwingWindowInteraction

    /**
     * Finds all composition-owned windows matching [matcher] (see [onWindow] for what
     * composition-owned means).
     */
    public fun onAllWindows(matcher: SwingMatcher): SwingWindowInteractionCollection
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
 * Finds the single composition-owned window (see [SwingUiTest.onWindow]). Convenience for the
 * common one-window composition:
 *
 * ```
 * setContent { Window(onCloseRequest = {}, title = "Main") { … } }
 * val frame = onWindow().fetch<JFrame>()
 * ```
 */
public fun SwingUiTest.onWindow(): SwingWindowInteraction = onWindow(SwingMatcher.any())

/**
 * Finds the single composition-owned window titled [title]. Convenience for
 * `onWindow(SwingMatcher.hasTitle(title))`.
 */
public fun SwingUiTest.onWindowWithTitle(title: String): SwingWindowInteraction = onWindow(SwingMatcher.hasTitle(title))

/**
 * Finds all composition-owned windows (see [SwingUiTest.onWindow]).
 */
public fun SwingUiTest.onAllWindows(): SwingWindowInteractionCollection = onAllWindows(SwingMatcher.any())

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
 * Runs with or without a display: the root is never attached to a [java.awt.Window], so no native
 * peer is realized and no UI is shown. The root is given a fixed size and laid out synchronously on
 * every idle pass, so tree/text/constraint AND bounds-based assertions (see
 * [SwingNodeInteraction.assertIsDisplayed]) all work off-screen.
 *
 * Content may also compose real top-level peers (`Window { }`, `Dialog { }`); those are found with
 * [SwingUiTest.onWindow] and are torn down with the composition when the block completes. Realizing
 * them requires a display — declare that requirement with a JUnit assumption
 * (`org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), …)`) at the top
 * of the block, so the test reports SKIPPED rather than failing on headless environments.
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
        // back to the recomposer between passes so it can recompose and apply changes.
        //
        // Each outer pass: publish pending snapshot writes, send exactly one frame, yield the EDT so
        // the recomposer coroutine runs, then force a synchronous layout pass. The gate returns only
        // when the composition is quiescent AND the EDT queue holds nothing that could revive it.
        // Idleness is never declared while a recomposition, a pending snapshot write, or an
        // already-scheduled EDT task could still produce observable work.
        var work = 0
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
            work++
            if (composed()) {
                // The composition is quiescent, but applying it may have left runnables scheduled on
                // the EDT that a single yield does not reach — a window/dialog defers its realization
                // to a later dispatch, and any such task can chain another invokeLater or wake a frame
                // awaiter that posts its own runnable. Drain until no scheduled runnable remains rather
                // than a fixed number of turns: a yield dispatches exactly the runnables queued before
                // its own continuation, so work scheduled during a yield lands after it and needs
                // another. Yielding is the drain step precisely because it advances queued work without
                // leaving any dispatch artifact behind.
                //
                // The termination condition tracks scheduled runnables only — the invocation events
                // that carry invokeLater callbacks and coroutine continuations — not every event on
                // the queue. A realized visible window peer streams native paint events indefinitely;
                // those never mutate composition state and must not be mistaken for pending work, or a
                // visible window would make the gate spin forever. So the gate returns once the
                // composition is quiescent AND no invocation is queued: no scheduled EDT callback and
                // no pending recomposition remain to revive observable work. If a dispatched runnable
                // instead revived the composition (a snapshot write, a fresh invalidation), we abandon
                // draining and let the next outer pass send a frame and recompose. MAX_IDLE_FRAMES
                // bounds the combined drains and frames so a runnable source that never quiesces fails
                // readably instead of spinning forever.
                while (composed()) {
                    if (noPendingInvocations()) return
                    yield()
                    Snapshot.sendApplyNotifications()
                    if (++work >= MAX_IDLE_FRAMES) break
                }
            }
            if (work >= MAX_IDLE_FRAMES) throw notSettled("awaitIdle")
        }
    }

    /** True once the composition itself is quiescent: no pending recomposition and no unpublished snapshot writes. */
    private fun composed(): Boolean = !recomposer.hasPendingWork && !Snapshot.current.hasPendingChanges()

    /**
     * True when no invocation event is queued on the EDT — no scheduled `invokeLater` callback and no
     * coroutine continuation awaiting dispatch. This is the "no scheduled runnable remains" signal the
     * idle gate drains toward. It deliberately ignores every other event class: a realized visible
     * window peer posts native paint events continuously, and treating those as pending work would
     * keep the gate spinning even though they never revive the composition.
     */
    private fun noPendingInvocations(): Boolean =
        java.awt.Toolkit
            .getDefaultToolkit()
            .systemEventQueue
            .peekEvent(java.awt.event.InvocationEvent.INVOCATION_DEFAULT) == null

    private fun notSettled(gate: String): AssertionError =
        AssertionError(
            "$gate did not settle after $MAX_IDLE_FRAMES frames: there is still pending " +
                "recomposition work, pending snapshot changes, or scheduled EDT work " +
                "(hasPendingWork=${recomposer.hasPendingWork}, " +
                "hasPendingChanges=${Snapshot.current.hasPendingChanges()}). " +
                "The composition likely never reaches a stable frame. Current tree:\n" +
                root.dumpTree() + realizedWindowsTreeDump(),
        )

    override suspend fun waitUntil(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ) {
        // Escape hatch (see interface KDoc). Bounded by BOTH a frame cap and a wall-clock deadline;
        // whichever trips first fails. Only frames the composition consumes count toward the cap, so
        // it is the deterministic bound on frame-driven work (a recomposition or frame-effect loop
        // that never meets the condition fails after a fixed number of frames regardless of machine
        // speed), while a condition gated on genuinely external timing — no compose work to consume
        // the frames — keeps being polled, with each yield dispatching arriving AWT events, until
        // the wall-clock deadline.
        val deadline = System.nanoTime() + timeoutMillis * 1_000_000
        var frames = 0
        while (true) {
            if (condition()) return
            if (frames >= MAX_WAIT_UNTIL_FRAMES || System.nanoTime() >= deadline) {
                throw AssertionError(
                    "Condition still not met after $frames consumed frames / ${timeoutMillis}ms. " +
                        "Current tree:\n" + root.dumpTree() + realizedWindowsTreeDump(),
                )
            }
            Snapshot.sendApplyNotifications()
            val consumesFrame = clock.hasAwaiters || recomposer.hasPendingWork
            frameTimeNanos += FRAME_INTERVAL_NANOS
            clock.sendFrame(frameTimeNanos)
            yield()
            layoutRoot()
            if (consumesFrame) frames++
        }
    }

    /**
     * Blocking variant of the idle gate used only by [setContent]'s one-shot initial settle. It pumps
     * frames and spins the EDT message queue inline (the recomposer runs as queued EDT tasks) without
     * suspending. Safe because it is called exactly once, before the test body needs the EDT for
     * anything else.
     */
    private fun settleBlocking() {
        // Same termination proof as awaitIdle, expressed inline: this variant cannot suspend, so it
        // drives its drain by pumping the EDT queue rather than yielding, but the loop shape and the
        // termination condition are identical. It returns only when the composition is quiescent AND a
        // pump found no scheduled runnable still queued, and never declares idleness while a scheduled
        // EDT callback or pending recomposition could still revive work. A pump advances only the work
        // queued before its exit marker, so work scheduled during a pump lands after it and needs
        // another pass; the loop keeps pumping until one pump both drains every queued invocation and
        // finds the composition quiescent. Like awaitIdle it tracks scheduled runnables only, never
        // the native paint events a visible window peer streams. MAX_IDLE_FRAMES bounds the combined
        // drains and frames as a runaway backstop.
        var work = 0
        while (true) {
            Snapshot.sendApplyNotifications()
            frameTimeNanos += FRAME_INTERVAL_NANOS
            clock.sendFrame(frameTimeNanos)
            val invocationsDrained = pumpEdtQueue()
            Snapshot.sendApplyNotifications()
            layoutRoot()
            work++
            if (composed() && invocationsDrained) return
            if (work >= MAX_IDLE_FRAMES) throw notSettled("setContent")
        }
    }

    /**
     * Lets Runnables already queued on the EDT (including the recomposer's apply step) run, without
     * leaving the EDT, and reports whether the pump drained every scheduled runnable. We are on the
     * EDT, so we cannot block on `invokeAndWait`; instead we enter an AWT [java.awt.SecondaryLoop] and
     * post a task that exits it. The secondary loop processes the pending events first, so the
     * recomposer's continuation runs before this returns.
     *
     * Whether any invocation remains is read from inside the exit task, at the one instant it is
     * honest: every event queued before the exit marker has been dispatched, and the secondary loop's
     * own teardown invocation has not yet been posted. A check taken after [enter] returns would
     * instead always see that teardown artifact and could never report a drained queue.
     */
    private fun pumpEdtQueue(): Boolean {
        val loop =
            java.awt.Toolkit
                .getDefaultToolkit()
                .systemEventQueue
                .createSecondaryLoop()
        // Post the exit AFTER the recomposer's already-queued continuation, so the loop drains those
        // first. enter() blocks the current EDT dispatch until exit() runs, while still pumping events.
        val drained = booleanArrayOf(false)
        javax.swing.SwingUtilities.invokeLater {
            drained[0] = noPendingInvocations()
            loop.exit()
        }
        loop.enter()
        return drained[0]
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
        SwingNodeInteraction(this, matcher.description, { root }, NodePick.Single) {
            root.findMatchingIncludingSelf(matcher)
        }

    override fun onAllNodesWithText(
        text: String,
        substring: Boolean,
    ): SwingNodeInteractionCollection = onAllNodes(SwingMatcher.hasText(text, substring))

    override fun onAllNodesWithTag(tag: String): SwingNodeInteractionCollection =
        onAllNodes(SwingMatcher.hasTestTag(tag))

    override fun onAllNodes(matcher: SwingMatcher): SwingNodeInteractionCollection =
        SwingNodeInteractionCollection(this, matcher, { root })

    override fun onRoot(): SwingNodeInteraction =
        SwingNodeInteraction(this, "root", { root }, NodePick.Single) {
            root.findMatchingIncludingSelf(SwingMatcher.isRoot(root))
        }

    override fun onWindow(matcher: SwingMatcher): SwingWindowInteraction =
        SwingWindowInteraction(this, matcher, description = matcher.description)

    override fun onAllWindows(matcher: SwingMatcher): SwingWindowInteractionCollection =
        SwingWindowInteractionCollection(matcher)

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
