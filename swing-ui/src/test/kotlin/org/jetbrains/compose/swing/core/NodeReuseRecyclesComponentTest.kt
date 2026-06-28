package org.jetbrains.compose.swing.core

import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Component
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Proves the behaviour the `ReusableComposeNode` switch buys: a node that is parked/reactivated or
 * moved across recompositions is **recycled**, not recreated. The same backing Swing [Component]
 * instance survives, and the holder's [org.jetbrains.compose.swing.core.SwingNodeHolder.onReuse] /
 * `onDeactivate` has drained the previous node's state so the recycled component reacts to the new
 * content rather than the old.
 *
 * Two genuine reuse paths are pinned to the *same instance*:
 *  - a [ReusableContentHost] child parked (`active = false`) and reactivated, and
 *  - a [movableContentOf] node moved between two slots.
 *
 * Note on `key()`: changing a `key()` argument is an explicit identity change, so the runtime
 * disposes the old keyed group and builds a fresh node (a new component instance) — that is NOT a
 * reuse and is covered for listener correctness by [ListenerReattachAfterReuseTest] instead. These
 * tests pin the *recycling* itself for the paths that truly recycle.
 */
class NodeReuseRecyclesComponentTest {
    /**
     * Resolves the single live AWT component whose text equals [text] by walking the real tree on
     * the EDT, so the test can compare instance identity across recompositions.
     */
    private fun SwingUiTest.componentWithText(text: String): Component = onNodeWithText(text).fetch<Component>()

    /**
     * A [movableContentOf] [Button] moved from NORTH to SOUTH keeps the same `JButton` instance: the
     * runtime relocates the existing node (deactivate in the old slot, reactivate in the new) rather
     * than building a fresh one, and the moved instance still fires its onClick.
     */
    @Test
    fun movingAMovableContentNodeKeepsTheSameComponentInstance() = runSwingUiTest {
        var counter by mutableIntStateOf(0)
        var inNorth by mutableStateOf(true)

        setContent {
            val button =
                remember {
                    movableContentOf {
                        Button(text = "Movable", onClick = { counter++ })
                    }
                }
            BorderPanel {
                if (inNorth) north { button() } else south { button() }
                center { Button(text = "anchor", onClick = {}) }
            }
        }

        val before = componentWithText("Movable")

        // Move NORTH -> SOUTH. The same node is relocated, not recreated.
        inNorth = false
        awaitIdle()

        val after = componentWithText("Movable")
        assertSame(
            before,
            after,
            "movableContent must relocate the SAME JButton instance across the move, not " +
                "allocate a new one",
        )

        // The relocated instance still reacts.
        onNodeWithText("Movable").performClick()
        check(counter == 1) { "Moved button's onClick did not fire; counter=$counter" }
    }

    /**
     * A button parked via [ReusableContentHost] (active = false) and then reactivated keeps the same
     * underlying component instance: deactivation detaches its listeners and reactivation reuses the
     * recycled node rather than building a new one.
     */
    @Test
    fun reusableContentHostReactivationKeepsTheSameComponentInstance() = runSwingUiTest {
        var counter by mutableIntStateOf(0)
        var active by mutableStateOf(true)

        setContent {
            BorderPanel {
                center {
                    ReusableContentHost(active = active) {
                        Button(text = "Recyclable", onClick = { counter++ })
                    }
                }
            }
        }

        val before = componentWithText("Recyclable")

        active = false
        awaitIdle()
        active = true
        awaitIdle()

        val after = componentWithText("Recyclable")
        assertSame(
            before,
            after,
            "A deactivated/reactivated ReusableContentHost child must reuse the same component " +
                "instance",
        )

        // And the recycled instance still reacts.
        onNodeWithText("Recyclable").performClick()
        check(counter == 1) { "Recycled button's onClick did not fire; counter=$counter" }
    }
}
