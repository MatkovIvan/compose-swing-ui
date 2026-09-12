@file:JvmMultifileClass
@file:JvmName("FoundationLayoutKt")

package org.jetbrains.compose.swing.foundation.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.swing.components.layout.updateLayout
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.node.SwingNode

/**
 * A composable that measures and places its [content] by a [MeasurePolicy] of your own: a container
 * whose placement rules you write, with no `LayoutManager` to go with them.
 *
 * [Row], [Column] and [Box] are each one of these; a policy you write is asked the same questions they
 * are. Children are written plainly, and the policy is handed one [Measurable] per child in declaration
 * order:
 *
 * ```
 * Layout({ measurables, constraints ->
 *     // A stack imposes no minimum of its own: each child takes the height left after earlier children.
 *     var remainingHeight = constraints.maxHeight
 *     val placeables = measurables.map { measurable ->
 *         val measured = measurable.measure(
 *             Constraints(maxWidth = constraints.maxWidth, maxHeight = remainingHeight)
 *         )
 *         // A layout modifier may escape an impossible offer, so give an overflowing child no room.
 *         val placeable =
 *             if (measured.width <= constraints.maxWidth && measured.height <= remainingHeight) {
 *                 measured
 *             } else {
 *                 measurable.measure(Constraints(maxWidth = 0, maxHeight = 0))
 *             }
 *         remainingHeight = (remainingHeight - placeable.height).coerceAtLeast(0)
 *         placeable
 *     }
 *     // The extent is named from the children and held inside the offer. Under an unbounded offer,
 *     // the same body answers the unbounded question `preferredLayoutSize` asks.
 *     val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
 *     val height = constraints.constrainHeight(constraints.maxHeight - remainingHeight)
 *     layout(width, height) {
 *         var y = 0
 *         for (placeable in placeables) {
 *             placeable.placeRelative(0, y)
 *             y += placeable.height
 *         }
 *     }
 * }) {
 *     Label(text = "Status")
 *     Button(text = "Close", onClick = ::close)
 * }
 * ```
 *
 * A child's own layout modifiers - a padding, an offset, an aspect ratio, a default minimum size -
 * stand between the constraints the policy offers and what the child is measured under, which is why
 * the content receiver is [ConstrainedScope]. Placements of your own go in a scope of your own
 * extending it, whose builders append a value the policy reads back through
 * [Measurable.layoutConstraint].
 *
 * @param measurePolicy how the container measures and places its children
 * @param modifier the [SwingModifier] applied to the panel
 * @param content the composable content of the container; see [ConstrainedScope]
 */
@Composable
public fun Layout(
    measurePolicy: MeasurePolicy,
    modifier: SwingModifier = SwingModifier,
    content: @Composable ConstrainedScope.() -> Unit,
) {
    SwingNode(
        factory = { MeasuredPanel(PolicyLayout(measurePolicy)) },
        modifier = modifier,
        update = { updateLayout<PolicyLayout, _>(measurePolicy) { policy = it } },
        content = { ConstrainedScopeImpl.content() },
    )
}

/**
 * The manager a [Layout] is built under: the policy its caller handed it, written in place when a
 * later pass hands a different one.
 */
internal class PolicyLayout(
    override var policy: MeasurePolicy,
) : MeasurePolicyLayout()

/** The [ConstrainedScope] a [Layout] hands its content, which offers no placement of its own. */
internal object ConstrainedScopeImpl : ConstrainedScope
