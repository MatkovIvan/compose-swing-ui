@file:JvmMultifileClass
@file:JvmName("LayoutComponentsKt")

package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.swing.SlotNode
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.constants.SplitOrientation
import org.jetbrains.compose.swing.core.SlotAttachment
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.applyModifier
import org.jetbrains.compose.swing.modifier.listener.propertyChangeListener
import java.beans.PropertyChangeListener
import javax.swing.JSplitPane

/**
 * A composable wrapper for `JSplitPane`, hosting two resizable sides separated by a draggable divider.
 *
 * Declare the two sides in [block]:
 * ```
 * SplitPane(orientation = JSplitPane.HORIZONTAL_SPLIT) {
 *     first { Navigator() }
 *     second { Editor() }
 * }
 * ```
 * Each side hosts exactly one child; redeclaring a side replaces its child, and dropping a side (e.g.
 * behind an `if`) clears it.
 *
 * The divider position is controlled: pass a pixel offset as [dividerLocation] to place the divider,
 * and [onDividerLocationChange] fires with the new offset whenever the divider moves. The default
 * `-1` is `JSplitPane`'s own initial divider location; as `setDividerLocation` documents, a negative
 * offset resets the divider to a position honoring the sides' preferred sizes (shaped by
 * [resizeWeight]).
 *
 * @param modifier the [SwingModifier] applied to the underlying `JSplitPane`
 * @param orientation the axis along which the two sides are arranged
 * @param dividerLocation the divider offset in pixels (controlled); a negative offset — the default
 *   `-1` is `JSplitPane`'s own initial divider location — resets the divider to honor the sides'
 *   preferred sizes
 * @param onDividerLocationChange callback invoked with the new offset when the divider moves
 * @param resizeWeight how extra space is shared when the pane resizes, from `0.0` (all to the second
 *   side) to `1.0` (all to the first side)
 * @param block declares the two sides; see [SplitPaneScope]
 */
@Composable
public fun SplitPane(
    modifier: SwingModifier = SwingModifier,
    @SplitOrientation orientation: Int = JSplitPane.HORIZONTAL_SPLIT,
    dividerLocation: Int = -1,
    onDividerLocationChange: (Int) -> Unit = {},
    resizeWeight: Double = 0.0,
    block: SplitPaneScope.() -> Unit,
) {
    val callback = rememberUpdatedState(onDividerLocationChange)
    val listener =
        remember { PropertyChangeListener { event -> callback.value((event.source as JSplitPane).dividerLocation) } }
    SplitPane(
        dividerLocationListener = listener,
        modifier = modifier,
        orientation = orientation,
        dividerLocation = dividerLocation,
        resizeWeight = resizeWeight,
        block = block,
    )
}

/**
 * A [SplitPane] driven by a raw [PropertyChangeListener] instead of an `onDividerLocationChange`
 * lambda. The listener is attached for the `dividerLocation` property as-is and removed on the same
 * instance; pass a stable instance (e.g. `remember {}`) to avoid churn.
 *
 * @param dividerLocationListener the listener notified when the `dividerLocation` property changes
 * @param modifier the [SwingModifier] applied to the underlying `JSplitPane`
 * @param orientation the axis along which the two sides are arranged
 * @param dividerLocation the divider offset in pixels (controlled); a negative offset — the default
 *   `-1` is `JSplitPane`'s own initial divider location — resets the divider to honor the sides'
 *   preferred sizes
 * @param resizeWeight how extra space is shared when the pane resizes
 * @param block declares the two sides; see [SplitPaneScope]
 */
@Composable
public fun SplitPane(
    dividerLocationListener: PropertyChangeListener,
    modifier: SwingModifier = SwingModifier,
    @SplitOrientation orientation: Int = JSplitPane.HORIZONTAL_SPLIT,
    dividerLocation: Int = -1,
    resizeWeight: Double = 0.0,
    block: SplitPaneScope.() -> Unit,
) {
    SplitPaneImpl(
        modifier = modifier,
        orientation = orientation,
        dividerLocation = dividerLocation,
        resizeWeight = resizeWeight,
        dividerListener = { pane ->
            pane.propertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, dividerLocationListener)
        },
        block = block,
    )
}

@Composable
private fun SplitPaneImpl(
    modifier: SwingModifier,
    @SplitOrientation orientation: Int,
    dividerLocation: Int,
    resizeWeight: Double,
    dividerListener: (SwingModifier) -> SwingModifier,
    block: SplitPaneScope.() -> Unit,
) {
    // Collect the side declarations fresh on every composition so a side the caller stops declaring
    // (e.g. behind an `if`) becomes null and its node is removed — the applier then uninstalls it and
    // clears the JSplitPane side. A remembered, mutated scope would retain the stale declaration.
    val scope = SplitPaneScopeImpl().apply(block)
    val pane = remember { JSplitPane() }

    SwingNode(
        factory = { pane },
        update = {
            set(orientation) { this.orientation = it }
            set(resizeWeight) { this.resizeWeight = it }
            // Apply the controlled location only when the composed value changes and differs from
            // the live position: a recomposition with an unchanged value never fights a user drag,
            // and a programmatic set never echoes back through the divider listener as a spurious
            // onDividerLocationChange. An explicit change to a negative offset writes through and
            // gets setDividerLocation's documented reset-to-preferred-sizes semantics.
            set(dividerLocation) { location ->
                if (this.dividerLocation != location) {
                    this.dividerLocation = location
                }
            }
            applyModifier(dividerListener(SwingModifier) then modifier)
        },
        content = {
            scope.first?.let { first ->
                val attachment = remember { splitSideAttachment(SplitSide.First) }
                SlotNode(attachment) { first() }
            }
            scope.second?.let { second ->
                val attachment = remember { splitSideAttachment(SplitSide.Second) }
                SlotNode(attachment) { second() }
            }
        },
    )
}

private class SplitPaneScopeImpl : SplitPaneScope {
    var first: (@Composable () -> Unit)? = null
        private set
    var second: (@Composable () -> Unit)? = null
        private set

    override fun first(block: @Composable () -> Unit) {
        first = block
    }

    override fun second(block: @Composable () -> Unit) {
        second = block
    }
}

/** Whether a side is the leading (`setLeftComponent`) or trailing (`setRightComponent`) one. */
private enum class SplitSide { First, Second }

/**
 * Installs a side's view into [side] of the host `JSplitPane`; uninstall clears that side.
 */
private fun splitSideAttachment(side: SplitSide): SlotAttachment =
    SlotAttachment { host, component, _ ->
        val pane = host as JSplitPane
        when (side) {
            SplitSide.First -> pane.leftComponent = component
            SplitSide.Second -> pane.rightComponent = component
        }
        return@SlotAttachment {
            when (side) {
                SplitSide.First -> pane.leftComponent = null
                SplitSide.Second -> pane.rightComponent = null
            }
        }
    }
