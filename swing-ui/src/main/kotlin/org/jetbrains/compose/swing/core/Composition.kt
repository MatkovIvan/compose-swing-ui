package org.jetbrains.compose.swing.core

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.runtime.staticCompositionLocalOf
import java.awt.Component
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Client property key for storing the [CompositionContext] associated with a component,
 * so that nested compositions can discover their parent and share its recomposition scope.
 */
internal const val COMPOSITION_KEY: String = "org.jetbrains.compose.swing.composition"

/**
 * Finds the parent [CompositionContext] by walking the Swing component tree, reading the
 * [COMPOSITION_KEY] client property off each [JComponent].
 *
 * The walk is **self-first**: the receiver itself is checked before its ancestors, so a component
 * stamped with a context (e.g. an interop host, or a window root pane) is discovered by a
 * `setContent` call on that very component, not only by its descendants.
 */
internal fun Component.findParentCompositionContext(): CompositionContext? {
    var current: Component? = this
    while (current != null) {
        if (current is JComponent) {
            (current.getClientProperty(COMPOSITION_KEY) as? CompositionContext)?.let { return it }
        }
        current = current.parent
    }
    return null
}

/**
 * Publishes [context] as [host]'s [COMPOSITION_KEY] client property so that descendant `setContent`
 * calls (and a self-first [findParentCompositionContext] on [host] itself) discover it as their parent.
 *
 * [host] may be `null` (a non-[JComponent] container has no client-property bag to stamp); the call is
 * then a no-op and the returned action does nothing.
 *
 * @return an idempotent action that clears the stamp. The caller invokes it from its own teardown.
 */
internal fun publishCompositionContext(
    host: JComponent?,
    context: CompositionContext,
): () -> Unit {
    if (host == null) return {}
    host.putClientProperty(COMPOSITION_KEY, context)
    return { host.putClientProperty(COMPOSITION_KEY, null) }
}

/**
 * Asserts the caller is on the Swing Event Dispatch Thread, failing loudly otherwise.
 *
 * Composition entry points and applier mutations must run on the EDT; calling them off-EDT
 * leads to non-deterministic corruption that is hard to diagnose, so we fail fast instead.
 */
internal fun checkEventDispatchThread() {
    check(SwingUtilities.isEventDispatchThread()) {
        "Compose-Swing must be used on the Event Dispatch Thread, but was called on " +
            "'${Thread.currentThread().name}'. Wrap the call in SwingUtilities.invokeLater { }."
    }
}

/**
 * Mounts a single island [Composition] as a child of a [CompositionContext].
 *
 * The island shares its parent's recomposition runtime (the parent context owns the recomposer,
 * clock, and scope); this mount owns only its [Composition] and its [SnapshotStateObserver]. Disposing
 * it disposes both — just this island's composition and its observer.
 *
 * The island is the composition owner for snapshot-observing components (e.g. `Canvas`): it owns one
 * [SnapshotStateObserver] shared by every such component in this composition, each registered as its
 * own scope. The applier stamps that observer onto each node it inserts, so a component reaches it from
 * its [SwingNodeHolder] rather than resolving a `CompositionLocal`.
 */
internal class SwingCompositionMount private constructor(
    private val composition: Composition,
    private val observer: SnapshotStateObserver,
) {
    fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
    }

    /** Disposes this island's [Composition] and stops its owner-level [SnapshotStateObserver]. */
    fun dispose() {
        composition.dispose()
        observer.stop()
        observer.clear()
    }

    companion object {
        /**
         * Mounts a child composition of [parent]. [applierFactory] builds the [Applier] over the
         * owner's freshly started [SnapshotStateObserver], which the applier then stamps onto every
         * node it inserts.
         */
        fun nested(
            parent: CompositionContext,
            applierFactory: (SnapshotStateObserver) -> Applier<*>,
        ): SwingCompositionMount {
            GlobalSnapshotManager.ensureStarted()
            // One observer shared by every snapshot-observing component (e.g. Canvas) in this owner. The
            // change callback runs directly: apply notifications are already pumped on the event dispatch
            // thread (see GlobalSnapshotManager) and a component reacts with repaint(), which is
            // thread-safe, so no extra thread marshaling is needed.
            val observer = SnapshotStateObserver { onChanged -> onChanged() }.apply { start() }
            return SwingCompositionMount(
                composition = Composition(applierFactory(observer), parent),
                observer = observer,
            )
        }
    }
}

/**
 * The parent-container layout constraint a child Swing node should be added with.
 *
 * A slot-based parent (e.g. a `BorderPanel`) provides the region for each slot, and [SwingNode] reads
 * this value into [SwingNodeHolder.constraint] so the applier adds the component with that constraint.
 * This lets the parent decide placement without the child knowing its container's layout manager.
 *
 * Defaults to `null`, meaning "add by index" (no explicit constraint).
 */
@PublishedApi
internal val LocalSwingConstraint: ProvidableCompositionLocal<Any?> = staticCompositionLocalOf { null }

/**
 * Carries a [SlotAttachment] down to the single [SwingNode] a slot hosts, the way
 * [LocalSwingConstraint] carries a layout constraint.
 *
 * A parent that owns a Swing host with dedicated single-occupancy slots (e.g. a `JScrollPane`'s
 * viewport / header / corner regions) provides this via [SlotNode]. [SwingNode] reads it into
 * [SwingNodeHolder.slotAttachment] so the applier installs the component through the attachment
 * instead of the generic `Container.add`. This lets the parent dictate how its content is attached
 * without the child knowing the host.
 *
 * Defaults to `null`, meaning "add as an ordinary child by index".
 */
@PublishedApi
internal val LocalSlotAttachment: ProvidableCompositionLocal<SlotAttachment?> =
    staticCompositionLocalOf { null }
