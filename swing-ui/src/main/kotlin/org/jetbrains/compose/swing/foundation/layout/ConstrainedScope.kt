package org.jetbrains.compose.swing.foundation.layout

import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.layout.AbsoluteOffsetElement
import org.jetbrains.compose.swing.modifier.layout.AbsolutePaddingElement
import org.jetbrains.compose.swing.modifier.layout.AspectRatioElement
import org.jetbrains.compose.swing.modifier.layout.DefaultMinSizeElement
import org.jetbrains.compose.swing.modifier.layout.OffsetElement
import org.jetbrains.compose.swing.modifier.layout.PaddingElement

/**
 * The scope of a container whose child stands between the constraints that container offers it and
 * its own measurement: a padding, an offset, an aspect ratio or a default minimum size each narrow
 * what reaches the child, state what the child plus its own room occupies, and place the child inside
 * that.
 *
 * [Row], [Column] and [Box] inherit this, so a child of any of them declares these alongside what
 * that container's own scope offers.
 */
@LayoutScopeMarker
public interface ConstrainedScope {
    /**
     * Sizes the child to [ratio] width per unit height, taking the size from the greatest width its
     * incoming constraints allow, then the greatest height, then the least width and the least
     * height, and stopping at the first of those that satisfies both the constraints and the ratio.
     * Where none of them does, the constraints are not respected: the child takes the size the first
     * of those extents that names a size at all implies at the ratio, and only where none of them
     * names one is the child measured under the incoming constraints unchanged.
     *
     * @param ratio the desired width to height ratio, finite and greater than zero
     * @param matchHeightConstraintsFirst takes the size from the greatest height, then the greatest
     *   width, then the least height and the least width, for a child whose height is the extent
     *   that should decide the other; `false` by default
     * @return this modifier with the aspect ratio declared on it.
     */
    public fun SwingModifier.aspectRatio(
        ratio: Float,
        matchHeightConstraintsFirst: Boolean = false,
    ): SwingModifier = this then AspectRatioElement(ratio, matchHeightConstraintsFirst)

    /**
     * Reserves [all] along every edge of the child.
     *
     * @return this modifier with the padding declared on it.
     */
    public fun SwingModifier.padding(all: Int): SwingModifier = this then PaddingElement(all, all, all, all)

    /**
     * Reserves [start] before the child and [end] after it along the reading order, and [top] and
     * [bottom] above and below it, each edge left unreserved by default. [start] and [end] swap edges
     * under a right-to-left reading order; see [absolutePadding] for a padding that never does.
     *
     * A padding reserves room, so none of the four is ever below zero; declare [offset] to move a
     * child outward from where its container places it.
     *
     * @return this modifier with the padding declared on it.
     */
    public fun SwingModifier.padding(
        start: Int = 0,
        top: Int = 0,
        end: Int = 0,
        bottom: Int = 0,
    ): SwingModifier = this then PaddingElement(start, top, end, bottom)

    /**
     * Reserves [horizontal] before and after the child along the reading order, and [vertical] above
     * and below it, either pair left unreserved by default.
     *
     * @return this modifier with the padding declared on it.
     */
    public fun SwingModifier.padding(
        horizontal: Int = 0,
        vertical: Int = 0,
    ): SwingModifier = padding(horizontal, vertical, horizontal, vertical)

    /**
     * Reserves [left], [top], [right] and [bottom] along the child's edges, each left unreserved by
     * default, the same under a right-to-left reading order as under a left-to-right one; see
     * [padding] for a padding that follows the reading order instead.
     *
     * None of the four is ever below zero, the same as for [padding].
     *
     * @return this modifier with the padding declared on it.
     */
    public fun SwingModifier.absolutePadding(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
    ): SwingModifier = this then AbsolutePaddingElement(left, top, right, bottom)

    /**
     * Moves the child by ([x], [y]) from where it would otherwise sit, neither axis moved by default,
     * without changing the room it measures into. A positive [x] moves the child toward the trailing
     * edge: right under a left-to-right reading order and left under a right-to-left one. See
     * [absoluteOffset] for an offset that always moves it toward the right.
     *
     * @return this modifier with the offset declared on it.
     */
    public fun SwingModifier.offset(
        x: Int = 0,
        y: Int = 0,
    ): SwingModifier = this then OffsetElement(x, y)

    /**
     * Moves the child by ([x], [y]) from where it would otherwise sit, neither axis moved by default,
     * the same under a right-to-left reading order as under a left-to-right one; see [offset] for an
     * offset that follows the reading order instead.
     *
     * @return this modifier with the offset declared on it.
     */
    public fun SwingModifier.absoluteOffset(
        x: Int = 0,
        y: Int = 0,
    ): SwingModifier = this then AbsoluteOffsetElement(x, y)

    /**
     * Raises the child's minimum size to [width] by [height] along whichever axis its incoming
     * constraints leave a minimum of zero on. An axis already claiming a minimum is left as it is,
     * and the minimum raised to is held between nothing and the room the child was offered, so a
     * child in a container smaller than the minimum takes the container.
     *
     * @throws IllegalArgumentException if [width] or [height] is negative
     * @return this modifier with the default minimum size declared on it.
     */
    public fun SwingModifier.defaultMinSize(
        width: Int,
        height: Int,
    ): SwingModifier = this then DefaultMinSizeElement(width, height)
}
