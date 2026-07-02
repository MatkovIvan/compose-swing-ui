package org.jetbrains.compose.swing.components.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.modifier.SwingModifier
import org.jetbrains.compose.swing.modifier.appearance.testTag
import org.jetbrains.compose.swing.modifier.layout.bounds
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JLayeredPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral tests for [LayeredPane] over a real [SwingApplier]. Each assertion reads the rendered
 * [JLayeredPane]: a declared child is hosted on it at its requested depth (`JLayeredPane.getLayer`),
 * children are added and removed dynamically as the composition changes, a child's layer re-applies on
 * recomposition, and disposing the pane tears it down.
 */
class LayeredPaneBehaviorTest {
    private fun SwingUiTest.layeredPane(): JLayeredPane = onNodeOfType<JLayeredPane>().fetch<JLayeredPane>()

    /** The depth layer the child carrying [tag] sits on, read from the pane that hosts it. */
    private fun SwingUiTest.layerOf(tag: String): Int = JLayeredPane.getLayer(onNodeWithTag(tag).fetch<JComponent>())

    @Test
    fun eachDeclaredChildIsHostedOnTheLayeredPaneAtItsLayer() = runSwingUiTest {
        setContent {
            LayeredPane {
                layer(JLayeredPane.DEFAULT_LAYER) { Label(text = "back", modifier = SwingModifier.testTag("back")) }
                layer(JLayeredPane.PALETTE_LAYER) { Label(text = "front", modifier = SwingModifier.testTag("front")) }
            }
        }

        val pane = layeredPane()
        assertEquals(2, pane.componentCount, "both children should be hosted on the layered pane")
        assertEquals(JLayeredPane.DEFAULT_LAYER, layerOf("back"), "the back child should sit on the default layer")
        assertEquals(JLayeredPane.PALETTE_LAYER, layerOf("front"), "the front child should sit on the palette layer")
        onNodeWithText("back").assertExists()
        onNodeWithText("front").assertExists()
    }

    @Test
    fun rawIntegerLayerIsHonored() = runSwingUiTest {
        setContent {
            LayeredPane {
                layer(7) { Label(text = "seven", modifier = SwingModifier.testTag("seven")) }
            }
        }

        assertEquals(7, layerOf("seven"))
    }

    @Test
    fun boundsModifierPositionsAChildWithinTheLayeredPane() = runSwingUiTest {
        setContent {
            LayeredPane {
                layer(JLayeredPane.DEFAULT_LAYER) {
                    Label(text = "fixed", modifier = SwingModifier.testTag("fixed").bounds(15, 25, 120, 40))
                }
            }
        }

        // A JLayeredPane has no layout manager, so the child keeps the bounds the modifier set.
        assertEquals(Rectangle(15, 25, 120, 40), onNodeWithTag("fixed").fetch<JComponent>().bounds)
    }

    @Test
    fun droppingAChildFromCompositionRemovesItDynamically() = runSwingUiTest {
        var showTop by mutableStateOf(true)
        setContent {
            LayeredPane {
                layer(JLayeredPane.DEFAULT_LAYER) { Label(text = "base") }
                if (showTop) {
                    layer(JLayeredPane.PALETTE_LAYER) { Label(text = "top") }
                }
            }
        }

        val pane = layeredPane()
        assertEquals(2, pane.componentCount, "both children should be hosted before dropping one")

        showTop = false
        awaitIdle()
        assertEquals(1, pane.componentCount, "dropped child was not removed")
        onNodeWithText("top").assertDoesNotExist()
        onNodeWithText("base").assertExists()

        showTop = true
        awaitIdle()
        assertEquals(2, pane.componentCount, "re-added child did not return")
        onNodeWithText("top").assertExists()
    }

    @Test
    fun changingAChildsLayerReappliesItOnRecomposition() = runSwingUiTest {
        var depth by mutableIntStateOf(JLayeredPane.DEFAULT_LAYER)
        setContent {
            LayeredPane {
                layer(depth) { Label(text = "mover", modifier = SwingModifier.testTag("mover")) }
            }
        }

        assertEquals(JLayeredPane.DEFAULT_LAYER, layerOf("mover"), "the child should start on the default layer")

        depth = JLayeredPane.DRAG_LAYER
        awaitIdle()
        assertEquals(JLayeredPane.DRAG_LAYER, layerOf("mover"), "child layer did not update on recomposition")
        onNodeWithText("mover").assertExists()
    }

    @Test
    fun disposingTheLayeredPaneTearsItDown() = runSwingUiTest {
        var show by mutableStateOf(true)
        setContent {
            if (show) {
                LayeredPane {
                    layer(JLayeredPane.DEFAULT_LAYER) { Label(text = "child") }
                }
            }
        }

        layeredPane()
        onNodeWithText("child").assertExists()

        show = false
        awaitIdle()

        val panes = onAllNodesOfType<JLayeredPane>().fetchAll<JLayeredPane>()
        assertTrue(panes.isEmpty(), "JLayeredPane was not removed on dispose.")
        onNodeWithText("child").assertDoesNotExist()
    }
}
