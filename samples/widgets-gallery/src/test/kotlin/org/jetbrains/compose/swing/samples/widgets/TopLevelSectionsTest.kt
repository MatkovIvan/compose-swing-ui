package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

class TopLevelSectionsTest {
    @Test
    fun theWindowsSectionMountsWithBothPeersClosed() =
        runSwingUiTest {
            openSection("Top-level windows")

            onNodeWithText("Window is closed", substring = true).assertExists()
            onNodeWithText("No dialog acknowledged yet", substring = true).assertExists()
        }

    @Test
    fun theDialogsSectionMountsWithItsIdleEchoes() =
        runSwingUiTest {
            openSection("Standard dialogs")

            onNodeWithText("Status: Not shown yet", substring = true).assertExists()
            onNodeWithText("No answer yet", substring = true).assertExists()
            onNodeWithText("Nothing entered", substring = true).assertExists()
            onNodeWithText("No file chosen", substring = true).assertExists()
            onNodeWithText("No color chosen", substring = true).assertExists()
        }

    @Test
    fun theDesktopPaneAddsAndControlledClosesAnInternalFrame() =
        runSwingUiTest {
            openSection("Layered & MDI")

            onNodeWithText("Controlled closes: 0", substring = true).assertExists()
            onNodeWithText("Inspector frame", substring = true).assertDoesNotExist()

            onNodeWithText("Add frame").performClick()
            onNodeWithText("Inspector frame", substring = true).assertExists()

            onNodeWithText("Remove frame").performClick()
            onNodeWithText("Inspector frame", substring = true).assertDoesNotExist()
        }

    @Test
    fun theLayeredPaneStacksItsDepthLayers() =
        runSwingUiTest {
            openSection("Layered & MDI")

            onNodeWithText("Default layer", substring = true).assertExists()
            onNodeWithText("Palette layer", substring = true).assertExists()
            onNodeWithText("Drag layer (top)", substring = true).assertExists()
        }

    @Test
    fun theTraySectionMountsWithTheIconHidden() =
        runSwingUiTest {
            openSection("System tray")

            onNodeWithText("Tray icon: hidden", substring = true).assertExists()
            onNodeWithText("Last action: none", substring = true).assertExists()
            onNodeWithText("Notifications: on", substring = true).assertExists()
        }
}
