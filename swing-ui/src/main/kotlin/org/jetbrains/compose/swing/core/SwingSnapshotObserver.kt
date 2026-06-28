package org.jetbrains.compose.swing.core

import androidx.compose.runtime.snapshots.SnapshotStateObserver

/**
 * One snapshot observer shared by every snapshot-observing component in a single composition owner.
 *
 * It owns a single [SnapshotStateObserver] for the whole composition rather than one per component, so
 * adding components does not multiply the global apply-observer registrations the runtime must notify.
 * Each component registers itself as a distinct *scope*: snapshot reads it performs are tracked against
 * that scope, and a later change to any of them runs the scope's own reaction.
 *
 * The on-changed callback runs the supplied runnable **directly**: a component reacts by calling
 * [javax.swing.JComponent.repaint], which is thread-safe (it enqueues a dirty region with the Swing
 * `RepaintManager`, which then services it on the event dispatch thread), so no extra thread marshaling
 * is needed. Apply notifications that drive the underlying registration are already pumped on the event
 * dispatch thread by the running composition (see [GlobalSnapshotManager]).
 *
 * The observer is generic over the scope type; what a scope reads and how it reacts is decided by the
 * component, keeping component-specific concerns out of this class.
 *
 * Lifecycle: created and [start]ed when its composition is mounted, [dispose]d when that composition is
 * disposed.
 */
internal class SwingSnapshotObserver {
    private val observer = SnapshotStateObserver { onChanged -> onChanged() }

    /** Starts observing global snapshot writes. Called once when the composition is mounted. */
    fun start() {
        observer.start()
    }

    /**
     * Runs [block] under observation: every snapshot state read inside [block] is tracked against
     * [scope], so a later change to any of it runs [onValueChangedForScope] with [scope].
     */
    fun <T : Any> observeReads(
        scope: T,
        onValueChangedForScope: (T) -> Unit,
        block: () -> Unit,
    ) {
        observer.observeReads(scope, onValueChangedForScope, block)
    }

    /**
     * Drops the tracked reads for a single [scope] (e.g. when its node leaves the composition), so it
     * stops being notified. The shared observer keeps running for the other scopes.
     */
    fun clear(scope: Any) {
        observer.clear(scope)
    }

    /**
     * Stops observation and clears every tracked read, removing the global apply-observer registration.
     * Called once when the composition is disposed.
     */
    fun dispose() {
        observer.stop()
        observer.clear()
    }
}
