package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

/**
 * Behavioral coverage for the sections that front platform peers — Top-level windows, Standard dialogs,
 * Layered & MDI, and System tray. The harness runs headless (`-Djava.awt.headless=true`), so opening a real
 * top-level window, a modal dialog, a file/colour chooser, or a system-tray icon would raise a
 * HeadlessException; these tests therefore drive only the in-tree, non-peer behavior — the controlled
 * internal frames, and each section's hoisted echo state — which is what the showcase exposes through
 * the composition. The peer-opening buttons are exercised interactively in the runnable sample.
 */
class TopLevelSectionsTest {
    @Test
    fun theWindowsSectionMountsWithBothPeersClosed() =
        runSwingUiTest {
            openSection("Top-level windows")

            // No peer is realised on entry; both cards report their closed/idle echo.
            onNodeWithText("Window is closed", substring = true).assertExists()
            onNodeWithText("No dialog acknowledged yet", substring = true).assertExists()
        }

    @Test
    fun theDialogsSectionMountsWithItsIdleEchoes() =
        runSwingUiTest {
            openSection("Standard dialogs")

            // Each dialog card starts idle; the suspend dialog calls are driven only in the runnable app.
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

            // Internal frames are lightweight Swing components (not top-level peers), so they are safe to
            // drive headless. The extra frame's declaration is gated on state.
            onNodeWithText("Controlled closes: 0", substring = true).assertExists()
            onNodeWithText("Inspector frame", substring = true).assertDoesNotExist()

            onNodeWithText("Add frame").performClick()
            onNodeWithText("Inspector frame", substring = true).assertExists()

            // Toggling it back off routes through the controlled state and removes the frame again.
            onNodeWithText("Remove frame").performClick()
            onNodeWithText("Inspector frame", substring = true).assertDoesNotExist()
        }

    @Test
    fun theLayeredPaneStacksItsDepthLayers() =
        runSwingUiTest {
            openSection("Layered & MDI")

            // All three depth layers are mounted at once; the stacking order is what makes them visible.
            onNodeWithText("Default layer", substring = true).assertExists()
            onNodeWithText("Palette layer", substring = true).assertExists()
            onNodeWithText("Drag layer (top)", substring = true).assertExists()
        }

    @Test
    fun theTraySectionMountsWithTheIconHidden() =
        runSwingUiTest {
            openSection("System tray")

            // The Tray is gated behind the toggle and is only emitted while it is on; on entry there is
            // no tray side effect, so the section is safe headless and reports its hidden/idle echoes.
            onNodeWithText("Tray icon: hidden", substring = true).assertExists()
            onNodeWithText("Last action: none", substring = true).assertExists()
            onNodeWithText("Notifications: on", substring = true).assertExists()
        }
}
