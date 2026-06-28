package org.jetbrains.compose.swing.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

/**
 * Regression guards for the listener-reuse fix.
 *
 * The contract under test: when a node is reused / deactivated-then-reactivated,
 * [org.jetbrains.compose.swing.core.SwingNodeHolder] must end up with its framework listener re-attached
 * so the recycled component keeps reacting (clicks still fire onClick).
 *
 * All three reuse paths — **movableContent** move, **ReusableContentHost** reactivation, and
 * **`key()`-driven** reuse — are asserted live. The framework listener is installed as a modifier
 * element (the built-in domain listener merged into the node's chain), and
 * [org.jetbrains.compose.swing.core.SwingNodeHolder.reset] clears the node's modifier diff state on
 * reuse/deactivation — detaching the listener and dropping the cached chain — so the next
 * recomposition re-applies the chain and re-installs exactly one fresh listener (the historical
 * defect left the recycled component with zero listeners and thus dead).
 */
class ListenerReattachAfterReuseTest {
    /**
     * Moves a single [Button] between two BorderLayout regions via [movableContentOf]. The move
     * triggers reuse/deactivation on the underlying holder. After it lands in its new region the
     * click must still increment. This path is FIXED and asserted live.
     */
    @Test
    fun movableButtonStillFiresAfterBeingMoved() = runSwingUiTest {
        var counter by mutableIntStateOf(0)
        var inNorth by mutableStateOf(true)

        setContent {
            val button =
                remember {
                    movableContentOf {
                        Button(text = "Move me", onClick = { counter++ })
                    }
                }
            BorderPanel {
                if (inNorth) north { button() } else south { button() }
                center { Label(text = "anchor") }
            }
        }

        // Click before the move: establishes the listener works initially.
        onNodeWithText("Move me").performClick()
        check(counter == 1) { "pre-move click failed; counter=$counter" }

        // Force the move (NORTH -> SOUTH). The same component instance is reused in the new slot.
        inNorth = false
        awaitIdle()

        // The reused button must STILL fire its onClick — reset() cleared the modifier diff state
        // on the move, so the listener element re-installed when the chain re-applied.
        onNodeWithText("Move me").performClick()
        check(counter == 2) {
            "Reused (moved) button's onClick did not fire — listener was not re-attached. " +
                "counter=$counter"
        }
    }

    /**
     * A button deactivated and reactivated via [ReusableContentHost] must keep its listener: on
     * reactivation the recomposition re-attaches it and the holder reset no longer clobbers the
     * freshly re-attached listener. Asserts the click still fires after a deactivate/reactivate cycle.
     */
    @Test
    fun deactivatedButtonReattachesListenerOnReactivation() = runSwingUiTest {
        var counter by mutableIntStateOf(0)
        var active by mutableStateOf(true)

        setContent {
            BorderPanel {
                center {
                    ReusableContentHost(active = active) {
                        Button(text = "Reusable", onClick = { counter++ })
                    }
                }
            }
        }

        onNodeWithText("Reusable").performClick()
        check(counter == 1) { "initial click failed; counter=$counter" }

        active = false
        awaitIdle()

        active = true
        awaitIdle()

        onNodeWithText("Reusable").assertExists().performClick()
        check(counter == 2) {
            "Reactivated button's onClick did not fire — listener was not re-attached after " +
                "deactivation. counter=$counter"
        }
    }

    /**
     * Toggling a [androidx.compose.runtime.key] reuses the component instance but rebinds onClick;
     * the rebound listener must fire after the swap (same reset/re-attach ordering as the
     * ReusableContentHost case). Asserts the new onClick fires after the key change.
     */
    @Test
    fun keyedButtonReusesComponentAndRebindsOnClick() = runSwingUiTest {
        var counterA by mutableIntStateOf(0)
        var counterB by mutableIntStateOf(0)
        var useA by mutableStateOf(true)

        setContent {
            BorderPanel {
                center {
                    KeyedButton(
                        key = if (useA) "A" else "B",
                        onClick = { if (useA) counterA++ else counterB++ },
                    )
                }
            }
        }

        onNodeWithText("Keyed").performClick()
        check(counterA == 1 && counterB == 0) { "A=$counterA B=$counterB" }

        useA = false
        awaitIdle()

        onNodeWithText("Keyed").performClick()
        check(counterB == 1) {
            "Reused (keyed) button did not fire its new onClick — listener not re-attached. " +
                "A=$counterA B=$counterB"
        }
    }
}

@Composable
private fun KeyedButton(
    key: String,
    onClick: () -> Unit,
) {
    androidx.compose.runtime.key(key) {
        Button(text = "Keyed", onClick = onClick)
    }
}
