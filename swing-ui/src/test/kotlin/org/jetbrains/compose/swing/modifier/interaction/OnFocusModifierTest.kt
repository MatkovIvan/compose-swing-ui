package org.jetbrains.compose.swing.modifier.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.name
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.event.FocusEvent
import javax.swing.JComponent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Behavioral coverage for the [onFocus] interaction modifier. Focus gain/loss is driven by
 * dispatching real [FocusEvent]s at the live component; the test asserts the user's onGained/onLost
 * callbacks fire and that they stop once the element leaves the chain.
 */
class OnFocusModifierTest {
    // Headless components never realize a peer, so dispatching a FocusEvent is routed through the
    // KeyboardFocusManager and never reaches the component's own listeners. Instead drive the
    // registered FocusListeners directly — the observable proof that onFocus installed a working
    // listener that routes gain/loss to the user callbacks.
    private fun JComponent.fireFocusGained() {
        val event = FocusEvent(this, FocusEvent.FOCUS_GAINED)
        focusListeners.forEach { it.focusGained(event) }
    }

    private fun JComponent.fireFocusLost() {
        val event = FocusEvent(this, FocusEvent.FOCUS_LOST)
        focusListeners.forEach { it.focusLost(event) }
    }

    @Test
    fun onFocusFiresGainedAndLost() = runSwingUiTest {
        var gained = 0
        var lost = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier.name("b").onFocus(onGained = { gained++ }, onLost = { lost++ }),
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.fireFocusGained()
        assertEquals(1, gained, "focus gained should fire the gained callback once")
        assertEquals(0, lost, "focus gained should not fire the lost callback")

        button.fireFocusLost()
        assertEquals(1, gained, "the gained callback should not fire again on focus lost")
        assertEquals(1, lost, "focus lost should fire the lost callback once")
    }

    @Test
    fun onFocusStopsAfterItsElementIsRemoved() = runSwingUiTest {
        var enabled by mutableStateOf(true)
        var gained = 0
        setContent {
            Button(
                "X",
                modifier =
                    SwingModifier.name("b").let {
                        if (enabled) it.onFocus(onGained = { gained++ }) else it
                    },
            )
        }
        val button = onNodeWithName("b").fetch<JComponent>()

        button.fireFocusGained()
        assertEquals(1, gained, "the gained callback should fire while the modifier is present")

        enabled = false
        awaitIdle()
        // The element left the chain, so its focus listener is gone: firing whatever remains must
        // not reach the removed callback.
        button.fireFocusGained()
        assertEquals(1, gained, "the focus listener must be removed when its element leaves the chain")
    }
}
