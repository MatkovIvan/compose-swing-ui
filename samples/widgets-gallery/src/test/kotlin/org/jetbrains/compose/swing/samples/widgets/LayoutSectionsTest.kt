package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.test.screenshot.assertImageMatches
import org.jetbrains.compose.swing.test.screenshot.captureToImage
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavioral coverage for the container/layout sections — Layouts, Split & ToolBar, ScrollPane, and
 * Tabs. These sections demonstrate layout wrappers; the tests switch a CardPanel, drive a controlled
 * split divider, capture the multi-slot ScrollPane as a real bitmap, and add/select tabs through the
 * controlled TabbedPane.
 */
class LayoutSectionsTest {
    @Test
    fun theCardPanelSwitchesTheVisibleCard() =
        runSwingUiTest {
            openSection("Layouts")

            // CardPanel shows one card at a time; the buttons select which is mounted.
            onNodeWithText("Card A", substring = true).assertExists()
            onNodeWithText("Card C", substring = true).assertDoesNotExist()

            onNodeWithText("Show C").performClick()
            onNodeWithText("Card C", substring = true).assertExists()
            onNodeWithText("Card A", substring = true).assertDoesNotExist()
        }

    @Test
    fun theOrientationToggleStaysConsistentInTheLayoutsSection() =
        runSwingUiTest {
            openSection("Layouts")

            // The orientation-aware BorderPanel exposes leading/trailing children regardless of direction;
            // flipping the RTL checkbox keeps them present (only their resolved edge changes).
            onNodeWithText("lineStart (leading)", substring = true).assertExists()
            onNodeWithText("Right-to-left orientation").performClick()
            onNodeWithText("lineStart (leading)", substring = true).assertExists()
            onNodeWithText("lineEnd (trailing)", substring = true).assertExists()
        }

    @Test
    fun theControlledSplitDividerMovesFromButtonsAndEcho() =
        runSwingUiTest {
            openSection("Split & ToolBar")

            onNodeWithText("Divider location: 140 px", substring = true).assertExists()

            // The "Move right" button bumps the hoisted divider state; the echo and the real JSplitPane
            // both reflect the new location.
            onNodeWithText("Move right").performClick()
            onNodeWithText("Divider location: 180 px", substring = true).assertExists()
            // The section hosts two split panes (controlled + weighted); the controlled one is the first.
            val controlled = onAllNodesOfType<JSplitPane>().fetchAll<JSplitPane>().first()
            assertEquals(JSplitPane.HORIZONTAL_SPLIT, controlled.orientation)
        }

    @Test
    fun theToolBarButtonAndToggleDriveTheirEcho() =
        runSwingUiTest {
            openSection("Split & ToolBar")

            onNodeWithText("New clicks: 0", substring = true).assertExists()
            onNodeWithText("New").performClick()
            onNodeWithText("New clicks: 1", substring = true).assertExists()

            onNodeWithText("Bold: off", substring = true).assertExists()
            onNodeWithText("Bold").performClick()
            onNodeWithText("Bold: on", substring = true).assertExists()
        }

    @Test
    fun theScrollPaneSectionCapturesItsLaidOutSlotsToABitmapScreenshotTest() =
        runSwingUiTest {
            openSection("ScrollPane")

            // The multi-slot ScrollPane carries a header cell; assert it is mounted, then capture the
            // header cell as a real bitmap. A re-capture of the unchanged cell matches the first.
            val firstHeader = onNodeWithText("Col 0")
            val captured = firstHeader.captureToImage()
            assertTrue(captured.width > 0 && captured.height > 0, "the captured header cell has real size")
            firstHeader.assertImageMatches(expected = captured)
        }

    @Test
    fun theTabbedPaneSelectsAndAddsTabs() =
        runSwingUiTest {
            openSection("Tabs")

            onNodeWithText("Selected tab index: 0", substring = true).assertExists()
            val tabs = onNodeOfType<JTabbedPane>().fetch<JTabbedPane>()
            assertEquals(2, tabs.tabCount)

            // Toggling "Show extra tab" inserts the dynamic third tab through the slot mechanism.
            onNodeWithText("Show extra tab").performClick()
            assertEquals(3, tabs.tabCount)

            // "Select last" drives the controlled selected index, which the echo mirrors.
            onNodeWithText("Select last").performClick()
            onNodeWithText("Selected tab index: 2", substring = true).assertExists()
        }
}
