package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

/**
 * Behavioral coverage for the interaction-driven sections — Context menu, Data transfer, Dynamic
 * hierarchy, Composition locals, and Effects. Each section mounts through the navigation shell and is
 * driven through the gestures its public surface exposes (clicks, checkboxes, toggles), reading the
 * echo each section recomposes.
 */
class InteractionSectionsTest {
    @Test
    fun theContextMenuSectionMountsWithItsInitialEchoes() =
        runSwingUiTest {
            openSection("Context menu")

            // The popup itself opens in a separate top-level menu window on a right-click, outside the
            // headless test root; what the section exposes through the in-tree API is its status echoes,
            // which start at their defaults.
            onNodeWithText("Last action: none", substring = true).assertExists()
            onNodeWithText("Word wrap: on, line numbers: off", substring = true).assertExists()
        }

    @Test
    fun theDataTransferSectionMountsWithItsInitialEchoes() =
        runSwingUiTest {
            openSection("Data transfer")

            // The drop target starts empty; the clipboard card shows its seed text.
            onNodeWithText("Drop here: Nothing dropped yet", substring = true).assertExists()
            onNodeWithText("Copy me to the system clipboard", substring = true).assertExists()
        }

    @Test
    fun theStructuralToggleInsertsAndRemovesItsSubtree() =
        runSwingUiTest {
            openSection("Dynamic hierarchy")

            // The details subtree is composed only while the box is checked; toggling it inserts then
            // removes the whole subtree (heading, field, slider) from the tree.
            onNodeWithText("Details (this entire subtree was just inserted)", substring = true)
                .assertDoesNotExist()
            onNodeWithText("Show details panel").performClick()
            onNodeWithText("Details (this entire subtree was just inserted)", substring = true)
                .assertExists()
            onNodeWithText("Show details panel").performClick()
            onNodeWithText("Details (this entire subtree was just inserted)", substring = true)
                .assertDoesNotExist()
        }

    @Test
    fun theVisibleContrastKeepsTheSlotWhileTogglingVisibility() =
        runSwingUiTest {
            openSection("Dynamic hierarchy")

            // Unlike the structural toggle, the visible()-driven panel stays composed across toggles, so
            // its click-counter button survives and keeps counting.
            onNodeWithText("Clicked 0 time(s)", substring = true).performClick()
            onNodeWithText("Clicked 1 time(s)", substring = true).assertExists()
            // Hiding then re-showing it keeps the same state — the slot was never removed.
            onNodeWithText("Show via visible() modifier").performClick()
            onNodeWithText("Show via visible() modifier").performClick()
            onNodeWithText("Clicked 1 time(s)", substring = true).assertExists()
        }

    @Test
    fun theCompositionLocalSectionMountsItsAccentedLabels() =
        runSwingUiTest {
            openSection("Composition locals")

            // The accent flows down through unaware ancestors to the deep consumers; both accented labels
            // are mounted regardless of the chosen colour.
            onNodeWithText("Middle-level accented label", substring = true).assertExists()
            onNodeWithText("Inner-level accented label", substring = true).assertExists()
        }

    @Test
    fun theEffectsSectionDrivesDisposeAndDerivedState() =
        runSwingUiTest {
            openSection("Effects")

            // DisposableEffect: removing the child from composition fires its onDispose, which the parent
            // log reflects.
            onNodeWithText("Child has not left composition yet.", substring = true).assertExists()
            onNodeWithText("Keep child in composition").performClick()
            onNodeWithText("Child left composition.", substring = true).assertExists()

            // derivedStateOf: the level label is derived purely from the slider amount's band.
            onNodeWithText("Derived level: Medium", substring = true).assertExists()
        }
}
