package org.jetbrains.compose.swing.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.SwingNode
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.BorderLayout
import java.awt.ComponentOrientation
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavioral regression test for dynamic right-to-left layout driven by [componentOrientation].
 *
 * When the orientation of a [BorderLayout]-backed container is flipped between left-to-right and
 * right-to-left, the container must request a relayout so its orientation-aware `LINE_START`/
 * `LINE_END` children swap horizontal edges. The bug was that the [componentOrientation] modifier
 * set the property but never asked for a relayout, so a reactive orientation change did not move the
 * children.
 *
 * The test drives the change through the real public API ([SwingNode] + the [componentOrientation]
 * modifier, re-applied across a recomposition when the orientation state changes) and observes two
 * things deterministically under headless:
 *
 *  1. The container's `revalidate()` is invoked on the orientation change — the relayout request the
 *     fix adds. This is the assertion that catches the bug: a [JPanel] subclass counts the calls,
 *     which is reliable off-screen where Swing's own `revalidate()` plumbing is otherwise inert.
 *  2. After the forced layout pass, the two children have swapped horizontal edges — the user-visible
 *     consequence of the relayout.
 */
class LayoutOrientationTest {
    /**
     * A [BorderLayout] panel that counts how many times a relayout was requested on it.
     *
     * The counter is a `static` shared instance rather than an instance field because `JPanel`'s
     * superclass constructor (and `add`) can call `revalidate()` before this subclass's instance
     * fields are initialized; a `null` instance field would NPE there.
     */
    private class RevalidateCountingPanel(
        private val revalidateCount: AtomicInteger?,
    ) : JPanel(BorderLayout()) {
        override fun revalidate() {
            // Guard against super-constructor calls that run before the constructor parameter binds
            // (it is null on the JVM until super() returns).
            revalidateCount?.incrementAndGet()
            super.revalidate()
        }
    }

    @Test
    fun togglingComponentOrientationRequestsRelayoutAndSwapsLineStartLineEndEdges() = runSwingUiTest {
        var rtl by mutableStateOf(false)
        val revalidateCount = AtomicInteger(0)
        val leading = JLabel("leading")
        val trailing = JLabel("trailing")

        setContent {
            SwingNode(
                factory = {
                    RevalidateCountingPanel(revalidateCount).also {
                        it.add(leading, BorderLayout.LINE_START)
                        it.add(trailing, BorderLayout.LINE_END)
                    }
                },
                update = {
                    applyModifier(
                        SwingModifier.componentOrientation(
                            if (rtl) ComponentOrientation.RIGHT_TO_LEFT else ComponentOrientation.LEFT_TO_RIGHT,
                        ),
                    )
                },
            )
        }

        // Left-to-right baseline: LINE_START on the left, LINE_END on the right.
        val leadingLtr = leading.x
        val trailingLtr = trailing.x
        assertTrue(
            leadingLtr < trailingLtr,
            "Under LTR, LINE_START (x=$leadingLtr) should sit left of LINE_END (x=$trailingLtr).",
        )

        // Flip orientation through state; the modifier element is re-applied with the new value.
        val countBeforeToggle = revalidateCount.get()
        rtl = true
        awaitIdle()
        val countAfterToggle = revalidateCount.get()

        // Bug-catching assertion: the orientation change must have requested a relayout.
        assertTrue(
            countAfterToggle > countBeforeToggle,
            "Toggling componentOrientation must request a relayout (revalidate). " +
                "Count before=$countBeforeToggle after=$countAfterToggle — the container was " +
                "never invalidated, so a reactive RTL change would not re-lay-out.",
        )

        // Consequence: the children have swapped edges under RTL.
        val leadingRtl = leading.x
        val trailingRtl = trailing.x
        assertTrue(
            leadingRtl > trailingRtl,
            "After toggling to RTL, LINE_START (x=$leadingRtl) should sit right of LINE_END " +
                "(x=$trailingRtl).",
        )
    }
}
