package org.jetbrains.compose.swing.core

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionContext
import org.jetbrains.compose.swing.annotations.InternalSwingUiApi
import org.jetbrains.compose.swing.modifier.SwingModifierState
import org.jetbrains.compose.swing.modifier.resetModifierState
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent

/**
 * Installs a node's [Component] into a Swing host that owns its own attachment slot — a host whose
 * children go through a dedicated setter rather than the generic `Container.add` used by the
 * [SwingApplier]. Two host shapes are covered:
 * - **Single-occupancy slots**, e.g. `JScrollPane`'s viewport / header / corner regions, each reached
 *   via `setViewportView` / `setRowHeaderView` / `setColumnHeaderView` / `setCorner`. The [index] is
 *   always `0`.
 * - **Ordered multi-occupancy hosts**, e.g. a `JTabbedPane` whose tabs are placed at an index via
 *   `insertTab(title, icon, component, tip, index)`.
 *
 * [install] attaches the component and returns the action that detaches it again, releasing the host
 * slot when the node leaves. For a multi-occupancy host, the returned action should detach by
 * component identity (e.g. `JTabbedPane.remove(component)`) so it stays correct as sibling indices
 * shift.
 *
 * Marked [InternalSwingUiApi]; it may change without notice.
 */
@InternalSwingUiApi
public fun interface SlotAttachment {
    /**
     * Attaches [component] into [host] through the host's dedicated setter and returns the action
     * that detaches it again and releases the slot. [index] is the node's composition index among the
     * host's slot children — `0` for a single-occupancy slot, the insertion position for an ordered
     * multi-occupancy host.
     */
    public fun install(
        host: Container,
        component: Component,
        index: Int,
    ): () -> Unit
}

/**
 * The node type of [SwingApplier]: a wrapper around a Swing [Component] that implements
 * [ComposeNodeLifecycleCallback] so the Compose runtime can invoke lifecycle callbacks when a node
 * is released, reused (movableContent / slot reuse), or deactivated.
 *
 * Components built with [SwingNode] interact with it indirectly through [SwingNodeUpdater].
 *
 * Modifier-installed listeners (see [org.jetbrains.compose.swing.modifier.listener]) are detached
 * and the node's modifier-applied properties restored on release, reuse, and deactivation, so a
 * recycled slot starts from a clean baseline.
 *
 * Marked [InternalSwingUiApi]; it may change without notice in any release. See
 * `docs/CUSTOM-COMPONENTS.md`.
 */
@InternalSwingUiApi
public class SwingNodeHolder<out T : Component>(
    public val component: T,
) : ComposeNodeLifecycleCallback {
    /**
     * The parent-container constraint this node was last added with (e.g. a `BorderLayout`
     * region), or `null` to be added by index. Populated by [SwingNode] from [LocalSwingConstraint]
     * and read by [SwingApplier].
     */
    @PublishedApi
    internal var constraint: Any? = null

    /** Set by [SwingNode] from the user's `onRelease`. Invoked once, on final release. */
    @PublishedApi
    internal var releaseBlock: (() -> Unit)? = null

    /**
     * Diff state for the node's [SwingModifier] chain. Written by [applyModifier]; reset on
     * release/reuse/deactivation so a recycled node restores its modified properties and drops
     * modifier-installed listeners.
     *
     * Marked [InternalSwingUiApi]; it may change without notice in any release.
     */
    @InternalSwingUiApi
    @PublishedApi
    internal var modifierState: SwingModifierState? = null

    /**
     * Non-`null` when this node's [component] is installed into its parent through a dedicated Swing
     * setter rather than the generic `Container.add` (e.g. a `JScrollPane` region reached via
     * `setViewportView`/`setRowHeaderView`/`setColumnHeaderView`/`setCorner`).
     *
     * This is a structural property of the node, set once at creation, and is retained across
     * [reset]: a recycled node that fills a slot still fills it.
     */
    @PublishedApi
    internal var slotAttachment: SlotAttachment? = null

    /**
     * The teardown returned by [SlotAttachment.install] for a [slotAttachment]-backed node, invoked
     * when the [SwingApplier] removes or moves the node so the host slot is released. `null` while the
     * node is not installed.
     */
    internal var slotUninstall: (() -> Unit)? = null

    /**
     * The composition owner's shared [SwingSnapshotObserver], stamped onto this node by its applier at
     * insert (the way CMP's owner attaches itself to each node). A snapshot-observing component (e.g.
     * `Canvas`) reads it from here to register its paint reads, instead of resolving a
     * `CompositionLocal`.
     *
     * Like [slotAttachment] this is an owner-stable structural property: set once at insert and
     * retained across [reset], since a recycled node stays in the same composition owner.
     */
    internal var ownerObserver: SwingSnapshotObserver? = null

    /**
     * `true` while this node's [component] carries a [COMPOSITION_KEY] stamp published by
     * [hostSubcompositions] (the `hostsSubcompositions = true` opt-in on [SwingNode]), so the stamp
     * is cleared exactly once on release/reuse/deactivation.
     */
    private var hostsSubcompositions: Boolean = false

    /**
     * Publishes [context] as this node's [COMPOSITION_KEY] client property so a descendant component's
     * `setContent` discovers it and nests into the surrounding composition. Backs the
     * `hostsSubcompositions = true` opt-in on [SwingNode]; idempotent across recompositions for the
     * same node.
     *
     * The [component] must be a [JComponent]; otherwise this throws [IllegalStateException].
     */
    @PublishedApi
    internal fun hostSubcompositions(context: CompositionContext) {
        val host =
            component as? JComponent
                ?: error(
                    "SwingNode(hostsSubcompositions = true) requires the factory component to be a " +
                        "JComponent so descendant setContent calls can discover this composition via " +
                        "the COMPOSITION_KEY client property, but it was a " +
                        "'${component.javaClass.name}'. A non-JComponent cannot host subcompositions " +
                        "through the client-property walk.",
                )
        host.putClientProperty(COMPOSITION_KEY, context)
        hostsSubcompositions = true
    }

    private fun reset() {
        constraint = null
        // Release the host slot if this node is still installed through a dedicated setter.
        //
        // `slotUninstall` is non-null here exactly when the node reaches reset() WITHOUT having gone
        // through the applier's `remove`/`move`, which are the only other call sites that run and
        // null the handle. The concrete production case is whole-subtree disposal:
        // SwingApplier.onClear() tears the root subtree down via `Container.removeAll()` and just
        // clears its tracking maps — it does NOT walk the slot-hosting descendants to null their
        // handles — so a JScrollPane region child (or any slot-attached node) still carries a live
        // `slotUninstall` when the runtime then releases it and reset() runs. Calling it here
        // releases that host slot; skipping it would leak the region's reference to the now-detached
        // child. In the ordinary remove/move path the applier already nulled the handle, so this is a
        // plain no-op. The call is null-safe rather than `check`-guarded precisely because both the
        // null and non-null states are legitimate; nulling afterwards keeps it idempotent against a
        // defensive second reset(). The structural `slotAttachment` itself is intentionally NOT
        // cleared: a recycled node that fills a slot still fills it.
        slotUninstall?.invoke()
        slotUninstall = null
        // Clear any COMPOSITION_KEY stamp this node published for the hostsSubcompositions opt-in, so
        // a node leaving the composition (release) or being recycled for new content (reuse /
        // deactivate) never leaks a stale parent context to a descendant's setContent walk. Mirrors
        // the window recomposer's stamp-then-clear discipline. The upcoming `update` re-stamps if the
        // incoming node opts in again.
        if (hostsSubcompositions) {
            (component as? JComponent)?.putClientProperty(COMPOSITION_KEY, null)
            hostsSubcompositions = false
        }
        // Detaches every modifier-installed listener (including the built-in domain listener) and
        // restores modified properties, then clears the diff state so a reused or reactivated node
        // (ReusableComposeNode / movableContent / ReusableContent) re-installs its listeners and
        // re-applies its modifier from a clean baseline on the next `update`.
        resetModifierState()
    }

    override fun onRelease() {
        // reset() already detaches modifier-installed listeners and clears the diff state; here we
        // additionally run and drop the one-shot release block, since the node is leaving the
        // composition for good.
        reset()
        releaseBlock?.invoke()
        releaseBlock = null
    }

    override fun onReuse() {
        // The runtime is recycling this holder for a new node in the same reusable slot (a node
        // emitted via ReusableComposeNode whose group is reused across recompositions — e.g. a
        // ReusableContentHost reactivated after being parked, or structurally-identical content
        // replacing it in the same slot). The recycled holder must behave like a freshly created one
        // for the incoming content: drop the layout constraint, release any host slot it still
        // occupies, and detach every modifier-installed listener while clearing the diff state, so
        // the upcoming `update` re-applies the new node's constraint, slot, and modifier chain from a
        // clean baseline instead of inheriting the previous node's.
        reset()
    }

    override fun onDeactivate() {
        // The node moved into a deactivated (movableContent) holder. Detach listeners so it does
        // not keep reacting while parked; a later activation re-runs `update` and re-attaches.
        reset()
    }
}
