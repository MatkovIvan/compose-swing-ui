package org.jetbrains.compose.swing.foundation.layout

import java.awt.Component
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

/** A manager whose policy a test supplies outright, rather than one that is its own policy. */
internal class TestPolicyLayout(
    override val policy: MeasurePolicy,
) : MeasurePolicyLayout()

/** A component asking for one fixed extent and declaring no maximum of its own. */
internal class FixedSizeChild(
    private val width: Int = 0,
    private val height: Int = 0,
) : JPanel() {
    override fun getPreferredSize(): Dimension = Dimension(width, height)
}

/**
 * Where a row or a column's policy puts [children] when it is measured under [constraints] outright -
 * the entry point a caller reaches when nothing routes the question to `intrinsicSize` first, and the
 * only way to hand a policy an extent `layoutContainer` never offers, such as an unbounded axis.
 *
 * Each child is registered under what it declares, and the bounds come back in declaration order.
 */
internal fun measuredUnder(
    axis: LayoutAxis,
    constraints: Constraints,
    vararg children: Pair<Component, LinearConstraint?>,
): List<Rectangle> {
    val linear =
        LinearLayout(
            axis = axis,
            arrangement =
                if (axis == LayoutAxis.Horizontal) {
                    HorizontalAxisArrangement(Arrangement.Start)
                } else {
                    VerticalAxisArrangement(Arrangement.Top)
                },
            alignment =
                if (axis == LayoutAxis.Horizontal) {
                    VerticalAxisAlignment(Alignment.Top)
                } else {
                    HorizontalAxisAlignment(Alignment.Start)
                },
        )
    val panel =
        JPanel(
            TestPolicyLayout { measurables, _ ->
                with(linear) { PolicyMeasureScope.measure(measurables, constraints) }
            },
        )
    children.forEach { (child, declared) -> panel.add(child, declared) }
    panel.setSize(MEASURED_PANEL_EXTENT, MEASURED_PANEL_EXTENT)
    panel.doLayout()
    return panel.components.map { it.bounds }
}

/** Large enough that the panel holding a measured policy never itself decides an extent. */
private const val MEASURED_PANEL_EXTENT = 1000

/**
 * Runs [body] with [root] under a frame that grants it a peer - what makes a container hold between two
 * Swing calls what a pass measured - on the dispatch thread that frame lays its own content out on. The
 * frame is never shown, so nothing takes focus.
 */
internal fun peered(
    root: JComponent,
    body: () -> Unit,
) {
    val frame = JFrame()
    try {
        SwingUtilities.invokeAndWait {
            frame.contentPane.layout = null
            frame.contentPane.add(root)
            frame.addNotify()
            body()
        }
    } finally {
        SwingUtilities.invokeAndWait { frame.dispose() }
    }
}
