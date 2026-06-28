package org.jetbrains.compose.swing.components.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.setContent
import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Component
import java.awt.ComponentOrientation
import javax.swing.JScrollPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Behavioral tests for [ScrollPane], built on the internal externally-attached node mechanism.
 *
 * The central guarantee: content and headers/corners are hosted in the JScrollPane's pre-wired
 * viewport / installed header viewports / corner hosts — never added directly to the JScrollPane by
 * the applier. These tests assert against the real AWT tree (viewport view, header viewports, corner
 * components) on the EDT.
 */
class ScrollPaneBehaviorTest {
    /** Resolves the single [JScrollPane] in the tree, failing with a tree dump otherwise. */
    private fun SwingUiTest.scrollPane(): JScrollPane = onNodeOfType<JScrollPane>().fetch<JScrollPane>()

    /**
     * Finds a component with [text] in the tree rooted at [root], INCLUDING [root] itself. A single
     * composable Label becomes the JViewport's view directly (`viewport.setView(label)`), so the view
     * IS the label rather than a container holding it; this helper covers both shapes.
     */
    private fun SwingUiTest.findText(
        root: Component,
        text: String,
    ): Component? {
        val matcher = SwingMatcher.hasText(text)
        if (matcher.matches(root)) return root
        return onAllNodesWithText(text).within(root).fetchAll<Component>().singleOrNull()
    }

    @Test
    fun contentIsHostedInTheViewportViewNotAddedToTheScrollPane() = runSwingUiTest {
        setContent {
            ScrollPane {
                content { Label(text = "Body") }
            }
        }

        val pane = scrollPane()
        val view = pane.viewport.view
        assertNotNull(view, "viewport has no view; content was not hosted in the viewport")
        // The content label must be reachable through the viewport's view.
        assertNotNull(
            findText(view, "Body"),
            "content label not found under the viewport view",
        )
        // The content host is the viewport's view, never added directly to the JScrollPane:
        // the JScrollPane's only direct children are the Swing-owned regions (the viewport
        // itself, scrollbars), so none of them is the content host except via the viewport.
        val directChildren = pane.components.toList()
        assertTrue(
            directChildren.none { it === view },
            "content host was added directly to the JScrollPane instead of into the viewport",
        )
        assertTrue(directChildren.contains(pane.viewport), "viewport is not a direct child of the JScrollPane")
        onNodeWithText("Body").assertExists()
    }

    @Test
    fun swappingContentUpdatesTheViewportViewInPlace() = runSwingUiTest {
        var flag by mutableStateOf(true)
        setContent {
            ScrollPane {
                content {
                    if (flag) Label(text = "First") else Label(text = "Second")
                }
            }
        }

        val pane = scrollPane()
        // The viewport host is the same instance before and after the swap; only its view changes.
        val viewportBefore = pane.viewport
        onNodeWithText("First").assertExists()

        flag = false
        awaitIdle()

        onNodeWithText("Second").assertExists()
        onNodeWithText("First").assertDoesNotExist()
        assertSame(viewportBefore, pane.viewport, "viewport instance changed across content swap")
        assertNotNull(findText(pane.viewport.view, "Second"), "new content not in viewport")
    }

    @Test
    fun rowHeaderPresentInstallsTheHeaderViewportAndItsView() = runSwingUiTest {
        setContent {
            ScrollPane {
                content { Label(text = "Body") }
                rowHeader { Label(text = "RowHead") }
            }
        }

        val pane = scrollPane()
        val rowHeader = pane.rowHeader
        assertNotNull(rowHeader, "row header viewport was not installed")
        assertNotNull(rowHeader.view, "row header viewport has no view")
        assertNotNull(
            findText(rowHeader.view, "RowHead"),
            "row header content not hosted in the row header viewport",
        )
    }

    @Test
    fun columnHeaderPresentInstallsTheHeaderViewportAndItsView() = runSwingUiTest {
        setContent {
            ScrollPane {
                content { Label(text = "Body") }
                columnHeader { Label(text = "ColHead") }
            }
        }

        val pane = scrollPane()
        val columnHeader = pane.columnHeader
        assertNotNull(columnHeader, "column header viewport was not installed")
        assertNotNull(columnHeader.view, "column header viewport has no view")
        assertNotNull(
            findText(columnHeader.view, "ColHead"),
            "column header content not hosted in the column header viewport",
        )
    }

    @Test
    fun anAbsentHeaderInstallsNothing() = runSwingUiTest {
        setContent {
            ScrollPane {
                content { Label(text = "Body") }
            }
        }

        val pane = scrollPane()
        assertNull(pane.rowHeader, "row header viewport installed though no rowHeader was declared")
        assertNull(pane.columnHeader, "column header viewport installed though no columnHeader was declared")
        assertNull(pane.getCorner(JScrollPane.UPPER_LEFT_CORNER), "a corner was installed though none declared")
        assertNull(
            pane.getCorner(JScrollPane.UPPER_RIGHT_CORNER),
            "a corner was installed though none declared",
        )
    }

    @Test
    fun componentOrientationResolvesLeadingCornerUnderRightToLeft() = runSwingUiTest {
        // Declare the corner only after the pane is flipped to right-to-left, so it is installed
        // (setCorner resolves the leading/trailing key against the orientation at install time)
        // while the pane is RTL: the leading corner must then resolve to the right edge.
        var declareCorner by mutableStateOf(false)
        setContent {
            ScrollPane {
                content { Label(text = "Body") }
                if (declareCorner) {
                    corner(JScrollPane.UPPER_LEADING_CORNER) { Label(text = "CornerView") }
                }
            }
        }

        val pane = scrollPane()
        pane.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
        declareCorner = true
        awaitIdle()

        val cornerHost = pane.getCorner(JScrollPane.UPPER_RIGHT_CORNER)
        assertNotNull(cornerHost, "UpperLeading corner did not resolve to UPPER_RIGHT under RTL")
        assertNull(
            pane.getCorner(JScrollPane.UPPER_LEFT_CORNER),
            "UpperLeading corner should not occupy the left edge under RTL",
        )
        assertNotNull(
            findText(cornerHost, "CornerView"),
            "corner content not hosted in the corner host",
        )
    }

    @Test
    fun scrollbarPolicyMapsThrough() = runSwingUiTest {
        setContent {
            ScrollPane(verticalScrollbar = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) {
                content { Label(text = "Body") }
            }
        }

        val pane = scrollPane()
        assertEquals(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            pane.verticalScrollBarPolicy,
            "vertical scrollbar policy did not map through",
        )
    }

    @Test
    fun disposingTheScrollPaneTearsItDown() = runSwingUiTest {
        var show by mutableStateOf(true)
        setContent {
            if (show) {
                ScrollPane {
                    content { Label(text = "Body") }
                }
            }
        }

        scrollPane() // present initially
        onNodeWithText("Body").assertExists()

        show = false
        awaitIdle()

        val panes = onAllNodesOfType<JScrollPane>().fetchAll<JScrollPane>()
        assertTrue(panes.isEmpty(), "JScrollPane was not removed on dispose.")
        onNodeWithText("Body").assertDoesNotExist()
    }
}
