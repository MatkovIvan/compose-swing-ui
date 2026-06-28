package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Behavioral tests for the [BorderPanel] scope-based DSL.
 *
 * Every assertion reads the real AWT tree: each region's child must be attached to the panel's
 * [BorderLayout] under the matching constraint string ([BorderLayout.getConstraints]). The two
 * families — absolute compass (`north`/`south`/`east`/`west`/`center`) and orientation-aware
 * (`pageStart`/`pageEnd`/`lineStart`/`lineEnd`) — both map onto their own BorderLayout fields, so the
 * tests confirm the constraint Swing actually recorded rather than any internal slot bookkeeping.
 */
class BorderPanelDslTest {
    /** Resolves the single component with [text], failing with a tree dump otherwise. */
    private fun SwingUiTest.componentWithText(text: String): Component = onNodeWithText(text).fetch<Component>()

    /** The [BorderLayout] constraint [text]'s component was added with, read from its parent panel. */
    private fun SwingUiTest.constraintOf(text: String): Any? {
        val component = componentWithText(text)
        val parent = component.parent as JPanel
        return (parent.layout as BorderLayout).getConstraints(component)
    }

    @Test
    fun eachCompassRegionPlacesItsChildAtTheMatchingConstraint() = runSwingUiTest {
        setContent {
            BorderPanel {
                north { Label(text = "N") }
                south { Label(text = "S") }
                east { Label(text = "E") }
                west { Label(text = "W") }
                center { Label(text = "C") }
            }
        }

        assertEquals(BorderLayout.NORTH, constraintOf("N"), "the north region should place its child at NORTH")
        assertEquals(BorderLayout.SOUTH, constraintOf("S"), "the south region should place its child at SOUTH")
        assertEquals(BorderLayout.EAST, constraintOf("E"), "the east region should place its child at EAST")
        assertEquals(BorderLayout.WEST, constraintOf("W"), "the west region should place its child at WEST")
        assertEquals(BorderLayout.CENTER, constraintOf("C"), "the center region should place its child at CENTER")
    }

    @Test
    fun eachOrientationAwareRegionPlacesItsChildAtTheMatchingConstraint() = runSwingUiTest {
        setContent {
            BorderPanel {
                pageStart { Label(text = "PS") }
                pageEnd { Label(text = "PE") }
                lineStart { Label(text = "LS") }
                lineEnd { Label(text = "LE") }
                center { Label(text = "C") }
            }
        }

        assertEquals(BorderLayout.PAGE_START, constraintOf("PS"), "pageStart should place its child at PAGE_START")
        assertEquals(BorderLayout.PAGE_END, constraintOf("PE"), "pageEnd should place its child at PAGE_END")
        assertEquals(BorderLayout.LINE_START, constraintOf("LS"), "lineStart should place its child at LINE_START")
        assertEquals(BorderLayout.LINE_END, constraintOf("LE"), "lineEnd should place its child at LINE_END")
        assertEquals(BorderLayout.CENTER, constraintOf("C"), "the center region should place its child at CENTER")
    }

    @Test
    fun redeclaringARegionReplacesItsChildWithTheLastDeclaration() = runSwingUiTest {
        setContent {
            BorderPanel {
                north { Label(text = "first") }
                north { Label(text = "second") }
            }
        }

        // The last declaration wins: only "second" is attached, at NORTH; "first" never appears.
        assertEquals(BorderLayout.NORTH, constraintOf("second"))
        onNodeWithText("first").assertDoesNotExist()
        onNodeWithText("second").assertExists()
    }

    @Test
    fun droppingARegionClearsItsChildWhileSiblingsKeepTheirConstraints() = runSwingUiTest {
        var showNorth by mutableStateOf(true)
        setContent {
            BorderPanel {
                if (showNorth) {
                    north { Label(text = "N") }
                }
                center { Label(text = "C") }
            }
        }

        assertEquals(BorderLayout.NORTH, constraintOf("N"), "the north child should start at NORTH")
        assertEquals(BorderLayout.CENTER, constraintOf("C"), "the center child should start at CENTER")

        showNorth = false
        awaitIdle()

        // The dropped region's child is gone and the panel no longer reports a NORTH child; the
        // surviving CENTER child keeps its constraint.
        onNodeWithText("N").assertDoesNotExist()
        assertEquals(BorderLayout.CENTER, constraintOf("C"), "the surviving center child should keep its constraint")
        val panel = componentWithText("C").parent as JPanel
        val layout = panel.layout as BorderLayout
        assertNull(
            layout.getLayoutComponent(BorderLayout.NORTH),
            "NORTH region still holds a child after the region was dropped",
        )
    }

    @Test
    fun swappingARegionsChildKeepsItAttachedAtTheSameConstraint() = runSwingUiTest {
        var flag by mutableStateOf(true)
        setContent {
            BorderPanel {
                center {
                    if (flag) Label(text = "First") else Label(text = "Second")
                }
            }
        }

        assertEquals(BorderLayout.CENTER, constraintOf("First"), "the first child should sit at CENTER")

        flag = false
        awaitIdle()

        onNodeWithText("First").assertDoesNotExist()
        assertEquals(BorderLayout.CENTER, constraintOf("Second"), "the swapped child should stay at CENTER")
    }
}
