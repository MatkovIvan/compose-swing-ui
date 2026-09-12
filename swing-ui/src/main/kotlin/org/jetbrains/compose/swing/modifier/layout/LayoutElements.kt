package org.jetbrains.compose.swing.modifier.layout

import org.jetbrains.compose.swing.foundation.layout.Constraints
import org.jetbrains.compose.swing.foundation.layout.shrunkBy
import java.awt.Dimension
import java.awt.Point
import kotlin.math.roundToInt

/** [this] plus [amount], coerced into an extent that never overflows past [Int.MAX_VALUE] or below zero. */
private fun Int.grownBy(amount: Int): Int = (this.toLong() + amount).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

/**
 * This non-negative padding side plus [other], held to the largest geometry extent [Constraints] can
 * represent. A pair beyond that extent reserves all finite room instead of overflowing
 * negative and giving the child more room than its parent offered.
 */
private fun Int.reservedWith(other: Int): Int = (this.toLong() + other).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/**
 * Why a padding declaring [sides] reserves no room at all, for one whose edges are not all above zero.
 *
 * Narrowing subtracts the room reserved, so an edge below zero would hand the child a maximum larger
 * than its parent offered and then place it outside the parent.
 */
private fun roomBelowZero(sides: String): String =
    "A padding reserves room along the edges it names, and there is no room below none, but this one " +
        "declares $sides. To move a child outward from where its container places it, declare " +
        "offset() instead."

/**
 * The room `ConstrainedScope.padding` reserves along the child's edges, leading-edge relative: [start]
 * leads and [end] trails the child along the reading order, swapping places under a right-to-left one.
 */
internal data class PaddingElement(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
) : LayoutElement {
    init {
        require(start >= 0 && top >= 0 && end >= 0 && bottom >= 0) {
            roomBelowZero("start $start, top $top, end $end, bottom $bottom")
        }
    }

    override val name: String get() = "padding"

    override val declaredValues: Map<String, Any?>
        get() = mapOf("start" to start, "top" to top, "end" to end, "bottom" to bottom)

    private val horizontalRoom: Int get() = start.reservedWith(end)

    private val verticalRoom: Int get() = top.reservedWith(bottom)

    override fun narrow(constraints: Constraints): Constraints = constraints.shrunkBy(horizontalRoom, verticalRoom)

    override fun around(
        inner: Dimension,
        constraints: Constraints,
    ): Dimension =
        Dimension(
            constraints.constrainWidth(inner.width.grownBy(horizontalRoom)),
            constraints.constrainHeight(inner.height.grownBy(verticalRoom)),
        )

    override fun placeWithin(
        inner: Dimension,
        outer: Dimension,
        leftToRight: Boolean,
    ): Point = if (leftToRight) Point(start, top) else Point(end, top)
}

/**
 * The room `ConstrainedScope.absolutePadding` reserves along the child's edges: [left], [top], [right]
 * and [bottom], the same under either reading order.
 */
internal data class AbsolutePaddingElement(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) : LayoutElement {
    init {
        require(left >= 0 && top >= 0 && right >= 0 && bottom >= 0) {
            roomBelowZero("left $left, top $top, right $right, bottom $bottom")
        }
    }

    override val name: String get() = "absolutePadding"

    override val declaredValues: Map<String, Any?>
        get() = mapOf("left" to left, "top" to top, "right" to right, "bottom" to bottom)

    private val horizontalRoom: Int get() = left.reservedWith(right)

    private val verticalRoom: Int get() = top.reservedWith(bottom)

    override fun narrow(constraints: Constraints): Constraints = constraints.shrunkBy(horizontalRoom, verticalRoom)

    override fun around(
        inner: Dimension,
        constraints: Constraints,
    ): Dimension =
        Dimension(
            constraints.constrainWidth(inner.width.grownBy(horizontalRoom)),
            constraints.constrainHeight(inner.height.grownBy(verticalRoom)),
        )

    override fun placeWithin(
        inner: Dimension,
        outer: Dimension,
        leftToRight: Boolean,
    ): Point = Point(left, top)
}

/**
 * The move `ConstrainedScope.offset` applies to the child's placement, without changing the room it
 * measures into: [x] moves it toward the trailing edge under a left-to-right reading order and toward
 * the leading edge under a right-to-left one.
 */
internal data class OffsetElement(
    val x: Int,
    val y: Int,
) : LayoutElement {
    override val name: String get() = "offset"

    override val declaredValues: Map<String, Any?> get() = mapOf("x" to x, "y" to y)

    override fun placeWithin(
        inner: Dimension,
        outer: Dimension,
        leftToRight: Boolean,
    ): Point = if (leftToRight) Point(x, y) else Point(-x, y)
}

/**
 * The move `ConstrainedScope.absoluteOffset` applies to the child's placement: ([x], [y]), the same
 * under either reading order.
 */
internal data class AbsoluteOffsetElement(
    val x: Int,
    val y: Int,
) : LayoutElement {
    override val name: String get() = "absoluteOffset"

    override val declaredValues: Map<String, Any?> get() = mapOf("x" to x, "y" to y)

    override fun placeWithin(
        inner: Dimension,
        outer: Dimension,
        leftToRight: Boolean,
    ): Point = Point(x, y)
}

/**
 * The minimum `ConstrainedScope.defaultMinSize` raises the child's constraints to, along each axis
 * whose incoming minimum is zero. A constraint that already claims a minimum along an axis is left as
 * it is, and the minimum raised to is held between nothing and the incoming maximum.
 */
internal data class DefaultMinSizeElement(
    val width: Int,
    val height: Int,
) : LayoutElement {
    init {
        require(width >= 0 && height >= 0) {
            "defaultMinSize width and height must be zero or more, but were $width by $height."
        }
    }

    override val name: String get() = "defaultMinSize"

    override val declaredValues: Map<String, Any?> get() = mapOf("width" to width, "height" to height)

    override fun narrow(constraints: Constraints): Constraints {
        val minWidth = if (constraints.minWidth == 0) width.coerceIn(0, constraints.maxWidth) else constraints.minWidth
        val minHeight =
            if (constraints.minHeight == 0) height.coerceIn(0, constraints.maxHeight) else constraints.minHeight
        return Constraints(minWidth, constraints.maxWidth, minHeight, constraints.maxHeight)
    }
}

/**
 * The `ConstrainedScope.aspectRatio` the child is sized under: [ratio] width per unit height, taken
 * from the extents its incoming constraints name in the order [findSizeAt] tries them. Where none of
 * those names a size at all, the child is measured under the constraints unchanged.
 */
internal data class AspectRatioElement(
    val ratio: Float,
    val matchHeightConstraintsFirst: Boolean,
) : LayoutElement {
    init {
        require(ratio.isFinite() && ratio > 0) { "aspectRatio $ratio must be finite and greater than zero" }
    }

    override val name: String get() = "aspectRatio"

    override val declaredValues: Map<String, Any?>
        get() = mapOf("ratio" to ratio, "matchHeightConstraintsFirst" to matchHeightConstraintsFirst)

    override fun narrow(constraints: Constraints): Constraints {
        val size = constraints.findSizeAt(ratio, matchHeightConstraintsFirst) ?: return constraints
        return Constraints(size.width, size.width, size.height, size.height)
    }
}

/** An extent an aspect ratio can take a size from, the other following from the ratio. */
private enum class RatioExtent {
    MaxWidth,
    MaxHeight,
    MinWidth,
    MinHeight,
}

/** The extents tried in turn, widest first, and the same order with the two axes swapped. */
private val WIDTH_FIRST =
    listOf(RatioExtent.MaxWidth, RatioExtent.MaxHeight, RatioExtent.MinWidth, RatioExtent.MinHeight)
private val HEIGHT_FIRST =
    listOf(RatioExtent.MaxHeight, RatioExtent.MaxWidth, RatioExtent.MinHeight, RatioExtent.MinWidth)

/**
 * The size satisfying [ratio]: each extent tried in turn while the size it yields must satisfy the
 * constraints, then the same round again with that requirement dropped. `null` where none yields a
 * size at all, which leaves the child measured under the constraints it was offered.
 */
private fun Constraints.findSizeAt(
    ratio: Float,
    matchHeightConstraintsFirst: Boolean,
): Dimension? {
    val order = if (matchHeightConstraintsFirst) HEIGHT_FIRST else WIDTH_FIRST
    for (enforce in ENFORCED_THEN_NOT) {
        for (extent in order) {
            sizeAt(extent, ratio, enforce)?.let { return it }
        }
    }
    return null
}

/** The size the constraints yield at [extent], held to them where [enforce]. */
private fun Constraints.sizeAt(
    extent: RatioExtent,
    ratio: Float,
    enforce: Boolean,
): Dimension? =
    when (extent) {
        RatioExtent.MaxWidth -> tryMaxWidth(ratio, enforce)
        RatioExtent.MaxHeight -> tryMaxHeight(ratio, enforce)
        RatioExtent.MinWidth -> tryMinWidth(ratio, enforce)
        RatioExtent.MinHeight -> tryMinHeight(ratio, enforce)
    }

/** A size must satisfy the constraints on the first round and need not on the second. */
private val ENFORCED_THEN_NOT = booleanArrayOf(true, false)

private fun Constraints.tryMaxWidth(
    ratio: Float,
    enforce: Boolean,
): Dimension? {
    if (!hasBoundedWidth) return null
    val height = (maxWidth / ratio).roundToInt()
    return if (height > 0 && (!enforce || satisfies(maxWidth, height))) Dimension(maxWidth, height) else null
}

private fun Constraints.tryMaxHeight(
    ratio: Float,
    enforce: Boolean,
): Dimension? {
    if (!hasBoundedHeight) return null
    val width = (maxHeight * ratio).roundToInt()
    return if (width > 0 && (!enforce || satisfies(width, maxHeight))) Dimension(width, maxHeight) else null
}

private fun Constraints.tryMinWidth(
    ratio: Float,
    enforce: Boolean,
): Dimension? {
    val height = (minWidth / ratio).roundToInt()
    return if (height > 0 && (!enforce || satisfies(minWidth, height))) Dimension(minWidth, height) else null
}

private fun Constraints.tryMinHeight(
    ratio: Float,
    enforce: Boolean,
): Dimension? {
    val width = (minHeight * ratio).roundToInt()
    return if (width > 0 && (!enforce || satisfies(width, minHeight))) Dimension(width, minHeight) else null
}

/** Whether ([width], [height]) falls inside these constraints. */
private fun Constraints.satisfies(
    width: Int,
    height: Int,
): Boolean = width in minWidth..maxWidth && height in minHeight..maxHeight
