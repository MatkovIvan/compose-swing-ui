package org.jetbrains.compose.swing.foundation.layout

import org.jetbrains.compose.swing.modifier.layout.LayoutElement
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager2
import java.util.IdentityHashMap

/**
 * The layout manager a [MeasurePolicy] drives: it holds one [Measurable] per child, answers the three
 * extents `LayoutManager2` asks for from the policy, and lays the container out by what the policy
 * measured.
 *
 * Two of those extents route to [MeasurePolicy.intrinsicSize] and one to [MeasurePolicy.measure], which
 * are separate bodies rather than one under two arguments. `preferredLayoutSize` and `minimumLayoutSize`
 * have no extent for a policy to divide among its children, so they ask what the policy wants; of the
 * three, only `layoutContainer` has a rectangle. The other rectangle comes from outside: a parent
 * measuring this container offers one through [ChildMeasurables.measuredSize], which places nothing.
 *
 * The policy works inside the container's insets: it is handed the inner extent and places children
 * relative to the inner rectangle's own origin.
 */
internal abstract class MeasurePolicyLayout : LayoutManager2 {
    /** The policy this container is laid out by. */
    internal abstract val policy: MeasurePolicy

    /** One measurable per child, and the two ways the policy is run over them. */
    val measurables: ChildMeasurables = ChildMeasurables(this)

    /** The receiver the policy places its children in, rewritten at the start of each layout pass. */
    private val placement = InnerPlacementScope()

    /**
     * Records what [component] was registered under, for its policy to read back through
     * [Measurable.layoutConstraint].
     *
     * A policy that reads only constraints of its own kind overrides this and refuses the rest before
     * calling up, the way `BorderLayout` and `GridBagLayout` refuse one they cannot read.
     */
    override fun addLayoutComponent(
        component: Component,
        constraints: Any?,
    ) {
        measurables.of(component).layoutConstraint = constraints
    }

    override fun addLayoutComponent(
        name: String?,
        component: Component,
    ): Unit = Unit

    /**
     * Records the layout modifiers [component] is measured through, outermost first, for this
     * container's policy to measure it under. An empty chain is the ordinary child, measured directly.
     *
     * Declared apart from the constraint because the two are orthogonal: a constraint names a place in
     * this container, and the chain stands between the child and whatever constraints that place earns
     * it. Redeclared after every [addLayoutComponent], which gives up the measurable the chain lives on.
     */
    fun declareLayoutChain(
        component: Component,
        chain: List<LayoutElement>,
    ) {
        measurables.of(component).layoutChain = chain
    }

    /** Gives up what [component] was registered under, and the extent measured for it. */
    override fun removeLayoutComponent(component: Component) {
        measurables.forget(component)
    }

    override fun invalidateLayout(target: Container) {
        measurables.invalidate()
    }

    override fun preferredLayoutSize(parent: Container): Dimension =
        measurables.askedSize(parent, MeasureMode.Preferred)

    /**
     * Minimum extents are read fresh every time. A measurable holds the preferred extent only, and a
     * container is asked for its minimum once per validate rather than once per pass.
     */
    override fun minimumLayoutSize(parent: Container): Dimension = measurables.askedSize(parent, MeasureMode.Minimum)

    /** A policy-driven container takes any extent it is offered and places its children inside it. */
    override fun maximumLayoutSize(target: Container): Dimension = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

    /** What the container's first child reports; see [firstChildAlignment]. */
    override fun getLayoutAlignmentX(target: Container): Float = firstChildAlignment(target) { it.alignmentX }

    override fun getLayoutAlignmentY(target: Container): Float = firstChildAlignment(target) { it.alignmentY }

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val width = innerExtent(parent.width, insets.left, insets.right)
        val height = innerExtent(parent.height, insets.top, insets.bottom)
        val result = measurables.settledOn(parent, width, height)
        placement.begin(insets.left, insets.top, width, parent.componentOrientation)
        with(result) { placement.placeChildren() }
    }
}

/**
 * Replaces the constraint this container already holds [component] under without removing its
 * measurable. [MeasurePolicyLayout.addLayoutComponent] dispatches to the manager's own validation
 * and side effects, while any result settled under the old constraint must not be reused.
 */
internal fun MeasurePolicyLayout.replaceLayoutConstraint(
    component: Component,
    constraint: Any?,
) {
    addLayoutComponent(component, constraint)
    measurables.clearSettledResult()
}

/** The non-negative extent left after the insets on its two edges have taken their room. */
private fun innerExtent(
    extent: Int,
    firstInset: Int,
    secondInset: Int,
): Int = (extent.toLong() - insetSpan(firstInset, secondInset)).coerceAtLeast(0L).toInt()

/** The room two insets take, held to the largest extent the geometry APIs can represent. */
private fun insetSpan(
    first: Int,
    second: Int,
): Long = (first.toLong() + second).coerceIn(0L, Int.MAX_VALUE.toLong())

/**
 * [extent] with [added] room beside it. An extent already at [Int.MAX_VALUE] stays there rather than
 * wrapping past it: a policy naming that extent asks for everything there is, and the insets around it
 * cannot be more than everything.
 */
private fun widened(
    extent: Int,
    added: Int,
): Int = (extent.toLong() + added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/**
 * One container's children as its policy sees them, and the two ways that policy is run over them: for
 * an extent it is offered, and for one it is asked to name.
 *
 * A layout pass and an intrinsic walk take lists of their own, so asking a container what it prefers
 * while a layout pass is in flight does not take that pass's own list away.
 */
internal class ChildMeasurables(
    private val owner: MeasurePolicyLayout,
) {
    private val measurables = IdentityHashMap<Component, ChildMeasurable>()
    private val layoutPass = ArrayList<Measurable>()
    private val intrinsicWalk = ArrayList<Measurable>()

    /** What the last [measuredSize] settled on, while the children still hold the extents it granted. */
    private var measured: MeasureResult? = null

    /** Whether the invalidation arriving is the one this container's own parent causes by placing it. */
    private var beingPlaced: Boolean = false

    /**
     * Which extent a child that cannot be asked a constrained question answers with. The policy never
     * learns it: it is what tells one policy body apart from the same body run for a different question.
     */
    var mode: MeasureMode = MeasureMode.Measure
        private set

    /**
     * The measurable for [child], made on the first call. A child always reaches
     * `addLayoutComponent` - `Container.addImpl` hands a `LayoutManager2` a null constraint where the
     * caller named none - so this also stands for a child added while another manager was in place.
     *
     * A child this container has not held before gives up what the last pass settled on: that pass
     * measured the children of the moment, and this one is not among them.
     */
    fun of(child: Component): ChildMeasurable =
        measurables.getOrPut(child) {
            measured = null
            ChildMeasurable(child, this)
        }

    /** What [child] was registered under, or `null` where this container does not hold it. */
    fun declaredBy(child: Component): Any? = measurables[child]?.layoutConstraint

    /** Gives up the measurable for [child], for a child leaving the container. */
    fun forget(child: Component) {
        measurables.remove(child)
        measured = null
    }

    /** Gives up the result a constrained parent settled on without invalidating any child. */
    fun clearSettledResult() {
        measured = null
    }

    /** Gives up every extent measured, for a container whose layout has been invalidated. */
    fun invalidate() {
        for (measurable in measurables.values) measurable.invalidate()
        if (!beingPlaced) measured = null
    }

    /**
     * Runs [reshape] - this container being moved or resized - keeping what the last pass settled on.
     * A reshape invalidates the container it resizes, and that invalidation says nothing about what a
     * pass granted the children inside: they still hold the extents it measured them at. What they
     * prefer is a reading of their own and is given up, the same as under any other invalidation.
     */
    fun reshaped(reshape: () -> Unit) {
        beingPlaced = true
        try {
            reshape()
        } finally {
            beingPlaced = false
        }
    }

    /**
     * The children a layout pass over [parent] hands its policy, in declaration order, with the pass put
     * in the mode a measure under given constraints runs in.
     */
    fun layoutPassOf(parent: Container): List<Measurable> {
        mode = MeasureMode.Measure
        measured = null
        return gather(parent, layoutPass)
    }

    /**
     * What the policy occupies under [constraints], plus the insets it measured inside - the answer a
     * container's own [ConstrainedSize] gives its parent.
     *
     * Nothing is placed: the parent is deciding an extent, and the placement follows from the bounds it
     * then assigns, which is what `layoutContainer` runs.
     */
    fun measuredSize(
        parent: Container,
        constraints: Constraints,
    ): Dimension {
        val insets = parent.insets
        val horizontal = insetSpan(insets.left, insets.right).toInt()
        val vertical = insetSpan(insets.top, insets.bottom).toInt()
        val result =
            with(owner.policy) {
                PolicyMeasureScope.measure(layoutPassOf(parent), constraints.shrunkBy(horizontal, vertical))
            }
        measured = result
        return Dimension(widened(result.width, horizontal), widened(result.height, vertical))
    }

    /** What the policy asks for in [mode], plus the insets the policy measured inside. */
    fun askedSize(
        parent: Container,
        mode: MeasureMode,
    ): Dimension {
        val children = gather(parent, intrinsicWalk)
        val retainedMeasurements = children.map { (it as ChildMeasurable).retainMeasurement() }
        val retainedMode = this.mode
        val retainedResult = measured
        this.mode = mode
        measured = null
        val result =
            try {
                with(owner.policy) { PolicyMeasureScope.intrinsicSize(children) }
            } finally {
                children.forEachIndexed { index, child ->
                    (child as ChildMeasurable).restoreMeasurement(retainedMeasurements[index])
                }
                this.mode = retainedMode
                measured = retainedResult
            }
        val insets = parent.insets
        return Dimension(
            widened(result.width, insetSpan(insets.left, insets.right).toInt()),
            widened(result.height, insetSpan(insets.top, insets.bottom).toInt()),
        )
    }

    /**
     * What the policy settled on for the inner extent [parent] now holds: the pass a parent already ran
     * over this container, where that pass settled on this very extent, and a fresh one otherwise.
     *
     * A container its parent measured has run its policy once for the offer that placement came from,
     * and its children still hold the extents that pass granted them. Running the policy again for the
     * extent it settled on would divide that extent instead of the offer, so a share worked out from a
     * wider offer would shrink under the child it was granted to.
     */
    fun settledOn(
        parent: Container,
        width: Int,
        height: Int,
    ): MeasureResult {
        measured?.let { if (it.width == width && it.height == height) return it }
        return with(owner.policy) {
            PolicyMeasureScope.measure(layoutPassOf(parent), Constraints(width, width, height, height))
        }
    }

    private fun gather(
        parent: Container,
        into: ArrayList<Measurable>,
    ): List<Measurable> {
        into.clear()
        for (index in 0 until parent.componentCount) {
            into.add(of(parent.getComponent(index)))
        }
        return into
    }
}

/** Which extent a child answers with when the question reaches its own argument-less Swing call. */
internal enum class MeasureMode {
    /** A layout pass: the child takes the extent it prefers, held to the constraints it was measured under. */
    Measure,

    /** The container is being asked what it prefers, so each child answers with what it prefers. */
    Preferred,

    /** The container is being asked for its minimum, so each child answers with its own minimum. */
    Minimum,
}

/**
 * One child of a container driven by a [MeasurePolicy], and the handle the policy places it by: the two
 * are one object, so a pass allocates nothing per child and a measure overwrites the placeable the
 * measure before it handed out.
 *
 * A child offered one extent on both axes takes that extent and is asked nothing - neither the
 * argument-less question nor the constrained one, since whatever it answered would be discarded.
 *
 * The extent the child prefers is otherwise measured once and kept while the child still holds the
 * extent it was read at, so a pass with nothing to re-measure asks the child nothing. A placement that
 * resizes the child gives that reading up, as does an invalidation of the container. A child with no
 * peer is measured afresh: a child resized by anything other than this container reaches its manager by
 * invalidating that container, and AWT carries the invalidation up only to a container `isValid` reports
 * true for, which requires a peer. Without one none ever arrives, and the extent held here would be
 * stale for good.
 */
internal class ChildMeasurable(
    /** The component this stands for, which the container's own policy reads properties of. */
    val component: Component,
    private val owner: ChildMeasurables,
) : Measurable,
    Placeable {
    override var layoutConstraint: Any? = null

    /**
     * The layout modifiers this child is measured through, outermost first; see
     * `MeasurePolicyLayout.declareLayoutChain`.
     */
    var layoutChain: List<LayoutElement> = emptyList()

    override var width: Int = 0
        private set

    override var height: Int = 0
        private set

    /** The extent the component itself occupies, which a chain holds within the extent it states. */
    private var placedWidth: Int = 0
    private var placedHeight: Int = 0

    /** Where within that stated extent the chain puts the component. */
    private var offsetX: Long = 0L
    private var offsetY: Long = 0L

    private var preferred: Dimension? = null

    override fun measure(constraints: Constraints): Placeable {
        if (layoutChain.isEmpty()) {
            measureComponent(constraints)
            placedWidth = width
            placedHeight = height
            offsetX = 0L
            offsetY = 0L
            return this
        }
        return measureThroughChain(constraints)
    }

    /**
     * The component measured under what the chain leaves of [constraints], then each element in turn
     * stating what it occupies around what the one inside it settled on, and where it puts it.
     *
     * The extent the outermost element states is what this measurable reports, whether or not it falls
     * inside [constraints]; see [LayoutElement.around] for which elements hold it to what they were
     * offered.
     */
    private fun measureThroughChain(constraints: Constraints): Placeable {
        val offered = ArrayList<Constraints>(layoutChain.size)
        var inner = constraints
        for (element in layoutChain) {
            offered.add(inner)
            inner = element.narrow(inner)
        }
        measureComponent(inner)
        placedWidth = width
        placedHeight = height
        val leftToRight = component.parent?.componentOrientation?.isLeftToRight ?: true
        var extent = Dimension(width, height)
        offsetX = 0L
        offsetY = 0L
        for (index in layoutChain.indices.reversed()) {
            val element = layoutChain[index]
            val outer = element.around(extent, offered[index])
            val within = element.placeWithinCoordinate(extent, outer, leftToRight)
            offsetX += within.x
            offsetY += within.y
            extent = outer
        }
        width = extent.width
        height = extent.height
        return this
    }

    /** What the component itself answers under [constraints], with no chain between. */
    private fun measureComponent(constraints: Constraints) {
        if (constraints.minWidth == constraints.maxWidth && constraints.minHeight == constraints.maxHeight) {
            width = constraints.minWidth
            height = constraints.minHeight
            return
        }
        val constrained = component as? ConstrainedSize
        if (constrained != null && owner.mode == MeasureMode.Measure) {
            constrained.measure(constraints)
            width = constraints.constrainWidth(constrained.constrainedWidth)
            height = constraints.constrainHeight(constrained.constrainedHeight)
        } else {
            val extent = plainExtent()
            width = constraints.constrainWidth(extent.width)
            height = constraints.constrainHeight(extent.height)
        }
    }

    /** Lays the component out at the extent it was measured at, where the chain puts it within [x] and [y]. */
    fun placeAt(
        x: Long,
        y: Long,
    ) {
        if (component.width != placedWidth || component.height != placedHeight) invalidate()
        component.setBounds(
            saturateLayoutCoordinate(x + offsetX),
            saturateLayoutCoordinate(y + offsetY),
            placedWidth,
            placedHeight,
        )
    }

    /**
     * Where the baseline falls within the extent this measurable states. A chain asks the component at
     * the extent the component itself occupies, and moves the answer down by as much as it moves it.
     */
    override fun baseline(
        width: Int,
        height: Int,
    ): Int {
        if (layoutChain.isEmpty()) return component.getBaseline(width, height)
        val placement = baselinePlacement(width, height)
        val own = component.getBaseline(placement.componentSize.width, placement.componentSize.height)
        return if (own < 0) own else saturateLayoutCoordinate(own.toLong() + placement.offsetY)
    }

    /**
     * The component's extent and displacement inside a chained measurable whose outer extent is
     * [width] by [height]. This mirrors [measureThroughChain] without consulting its last result:
     * baseline alignment asks about the dimensions it supplies, not the dimensions a prior measure
     * happened to leave behind.
     */
    private fun baselinePlacement(
        width: Int,
        height: Int,
    ): BaselinePlacement {
        val offered = ArrayList<Constraints>(layoutChain.size)
        var constraints = Constraints(width, width, height, height)
        for (element in layoutChain) {
            offered.add(constraints)
            constraints = element.narrow(constraints)
        }
        val componentSize = Dimension(constraints.minWidth, constraints.minHeight)
        var extent = componentSize
        var offsetY = 0L
        val leftToRight = component.parent?.componentOrientation?.isLeftToRight ?: true
        for (index in layoutChain.indices.reversed()) {
            val element = layoutChain[index]
            val outer = element.around(extent, offered[index])
            val within = element.placeWithinCoordinate(extent, outer, leftToRight)
            offsetY += within.y
            extent = outer
        }
        return BaselinePlacement(componentSize, offsetY)
    }

    /** Gives up the extent measured, for a container whose layout has been invalidated. */
    fun invalidate() {
        preferred = null
    }

    /** What the child answers through its own argument-less Swing call, for the mode the pass is in. */
    private fun plainExtent(): Dimension =
        when (owner.mode) {
            MeasureMode.Minimum -> component.minimumSize
            else -> if (component.isDisplayable) preferred ?: measurePreferred() else component.preferredSize
        }

    private fun measurePreferred(): Dimension = component.preferredSize.also { preferred = it }

    /** Captures the mutable placeable state while an intrinsic query temporarily remeasures this child. */
    fun retainMeasurement(): ChildMeasurement =
        ChildMeasurement(width, height, placedWidth, placedHeight, offsetX, offsetY)

    /** Restores the placeable a retained layout result expects after an intrinsic query. */
    fun restoreMeasurement(measurement: ChildMeasurement) {
        width = measurement.width
        height = measurement.height
        placedWidth = measurement.placedWidth
        placedHeight = measurement.placedHeight
        offsetX = measurement.offsetX
        offsetY = measurement.offsetY
    }
}

/** The mutable geometry one [ChildMeasurable] must preserve across an intrinsic query. */
internal data class ChildMeasurement(
    val width: Int,
    val height: Int,
    val placedWidth: Int,
    val placedHeight: Int,
    val offsetX: Long,
    val offsetY: Long,
)

/** The component size and vertical displacement a chained measurable derives for a baseline query. */
private data class BaselinePlacement(
    val componentSize: Dimension,
    val offsetY: Long,
)

/** The component a policy of this library reads a property of - a maximum size, a baseline. */
internal val Measurable.component: Component get() = (this as ChildMeasurable).component

/**
 * What a measure of this child settled on. One object is both, so this is the placeable the child's own
 * [Measurable.measure] returned, and a policy reaches it without holding what it handed back.
 */
internal val Measurable.measured: Placeable get() = this as ChildMeasurable

/**
 * The reading order the container carries, which a policy of this library resolves an [Arrangement] and
 * an [Alignment] against. [PlacementScope.isLeftToRight] is the whole of it a policy outside the library
 * needs; these two take the orientation itself.
 */
internal val PlacementScope.orientation: ComponentOrientation get() = (this as InnerPlacementScope).orientation

/**
 * Where a policy places its children: inside the container's insets, whose origin every placement is
 * offset by, so a policy works in the coordinates it measured in.
 */
internal class InnerPlacementScope : PlacementScope {
    private var originX: Int = 0
    private var originY: Int = 0

    /** The reading order the container carries; see [orientation]. */
    var orientation: ComponentOrientation = ComponentOrientation.LEFT_TO_RIGHT
        private set

    override var parentWidth: Int = 0
        private set

    override val isLeftToRight: Boolean get() = orientation.isLeftToRight

    fun begin(
        originX: Int,
        originY: Int,
        parentWidth: Int,
        orientation: ComponentOrientation,
    ) {
        this.originX = originX
        this.originY = originY
        this.parentWidth = parentWidth
        this.orientation = orientation
    }

    override fun Placeable.place(
        x: Int,
        y: Int,
    ) {
        (this as ChildMeasurable).placeAt(
            originX.toLong() + x.toLong(),
            originY.toLong() + y.toLong(),
        )
    }

    override fun Placeable.placeRelative(
        x: Int,
        y: Int,
    ) {
        val relativeX = if (isLeftToRight) x.toLong() else parentWidth.toLong() - width.toLong() - x.toLong()
        (this as ChildMeasurable).placeAt(
            originX.toLong() + relativeX,
            originY.toLong() + y.toLong(),
        )
    }
}
