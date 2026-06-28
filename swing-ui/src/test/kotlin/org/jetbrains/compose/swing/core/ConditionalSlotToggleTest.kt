package org.jetbrains.compose.swing.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.layout.BorderPanel
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.BorderLayout
import java.awt.Component
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression guard for the disappearing-slot applier bug. A [BorderPanel] has a CONDITIONAL NORTH
 * slot driven by a boolean state, plus stable CENTER and SOUTH slots. The historical defect was that
 * a constrained child was added with `Container.add(Component, Object)`, which appends to the AWT
 * component array regardless of the composition index. Toggling the conditional NORTH slot then
 * desynced the array order from the composition order, so the index-based remove/move addressed the
 * wrong component: toggling NORTH off removed CENTER instead, and toggling it on placed it at the end
 * instead of composition index 0.
 *
 * These tests pin that the stable siblings keep their identity and constraints across a NORTH
 * on -> off cycle, and that turning NORTH on places it correctly without disturbing the siblings.
 */
class ConditionalSlotToggleTest {
    private companion object {
        const val NORTH_TEXT = "North"
        const val CENTER_TEXT = "Center"
        const val SOUTH_TEXT = "South"
    }

    /**
     * Resolves the single component whose text equals [text] by walking the real AWT tree on the EDT.
     * Returns the live instance so tests can assert identity is preserved across recompositions.
     */
    private fun SwingUiTest.componentWithText(text: String): Component = onNodeWithText(text).fetch<Component>()

    @Test
    fun togglingNorthOnThenOffLeavesSiblingsIntactAndNorthGone() = runSwingUiTest {
        var showNorth by mutableStateOf(false)
        setContent {
            BorderPanel {
                if (showNorth) {
                    north { Label(text = NORTH_TEXT) }
                }
                center { Label(text = CENTER_TEXT) }
                south { Label(text = SOUTH_TEXT) }
            }
        }

        // Baseline (north off): center and south exist in their regions, north absent.
        onNodeWithText(CENTER_TEXT).assertExists().assertLayoutConstraint(BorderLayout.CENTER)
        onNodeWithText(SOUTH_TEXT).assertExists().assertLayoutConstraint(BorderLayout.SOUTH)
        onNodeWithText(NORTH_TEXT).assertDoesNotExist()

        // Capture the live sibling instances so we can prove identity survives the toggle cycle.
        val centerBefore = componentWithText(CENTER_TEXT)
        val southBefore = componentWithText(SOUTH_TEXT)

        // Turn NORTH on.
        showNorth = true
        awaitIdle()
        onNodeWithText(NORTH_TEXT).assertExists().assertLayoutConstraint(BorderLayout.NORTH)

        // Turn NORTH off again. The bug removed CENTER here instead of NORTH.
        showNorth = false
        awaitIdle()

        // NORTH is gone; CENTER and SOUTH still exist, in their correct regions, same instances.
        onNodeWithText(NORTH_TEXT).assertDoesNotExist()
        onNodeWithText(CENTER_TEXT).assertExists().assertLayoutConstraint(BorderLayout.CENTER)
        onNodeWithText(SOUTH_TEXT).assertExists().assertLayoutConstraint(BorderLayout.SOUTH)
        assertSame(centerBefore, componentWithText(CENTER_TEXT), "CENTER instance changed across toggle")
        assertSame(southBefore, componentWithText(SOUTH_TEXT), "SOUTH instance changed across toggle")
    }

    @Test
    fun togglingNorthOnPlacesItCorrectlyWithoutDisturbingSiblings() = runSwingUiTest {
        var showNorth by mutableStateOf(false)
        setContent {
            BorderPanel {
                if (showNorth) {
                    north { Label(text = NORTH_TEXT) }
                }
                center { Label(text = CENTER_TEXT) }
                south { Label(text = SOUTH_TEXT) }
            }
        }

        val centerBefore = componentWithText(CENTER_TEXT)
        val southBefore = componentWithText(SOUTH_TEXT)

        // Turn NORTH on: it must land at NORTH (not appended to the end as the bug did), and the
        // stable siblings must keep their regions and identities.
        showNorth = true
        awaitIdle()

        onNodeWithText(NORTH_TEXT).assertExists().assertLayoutConstraint(BorderLayout.NORTH)
        onNodeWithText(CENTER_TEXT).assertExists().assertLayoutConstraint(BorderLayout.CENTER)
        onNodeWithText(SOUTH_TEXT).assertExists().assertLayoutConstraint(BorderLayout.SOUTH)
        assertSame(centerBefore, componentWithText(CENTER_TEXT), "CENTER instance changed when NORTH added")
        assertSame(southBefore, componentWithText(SOUTH_TEXT), "SOUTH instance changed when NORTH added")
    }
}
