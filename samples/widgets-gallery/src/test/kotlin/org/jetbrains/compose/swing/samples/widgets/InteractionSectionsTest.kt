package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import kotlin.test.Test

class InteractionSectionsTest {
    @Test
    fun theContextMenuSectionMountsWithItsInitialEchoes() =
        runSwingUiTest {
            openSection("Context menu")

            onNodeWithText("Last action: none", substring = true).assertExists()
            onNodeWithText("Word wrap: on, line numbers: off", substring = true).assertExists()
        }

    @Test
    fun theDataTransferSectionMountsWithItsInitialEchoes() =
        runSwingUiTest {
            openSection("Data transfer")

            onNodeWithText("Drop here: Nothing dropped yet", substring = true).assertExists()
            onNodeWithText("Copy me to the system clipboard", substring = true).assertExists()
        }

    @Test
    fun theStructuralToggleInsertsAndRemovesItsSubtree() =
        runSwingUiTest {
            openSection("Dynamic hierarchy")

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

            onNodeWithText("Clicked 0 time(s)", substring = true).performClick()
            onNodeWithText("Clicked 1 time(s)", substring = true).assertExists()
            onNodeWithText("Show via visible() modifier").performClick()
            onNodeWithText("Show via visible() modifier").performClick()
            onNodeWithText("Clicked 1 time(s)", substring = true).assertExists()
        }

    @Test
    fun theCompositionLocalSectionMountsItsAccentedLabels() =
        runSwingUiTest {
            openSection("Composition locals")

            onNodeWithText("Middle-level accented label", substring = true).assertExists()
            onNodeWithText("Inner-level accented label", substring = true).assertExists()
        }

    @Test
    fun theEffectsSectionDrivesDisposeAndDerivedState() =
        runSwingUiTest {
            openSection("Effects")

            onNodeWithText("Child has not left composition yet.", substring = true).assertExists()
            onNodeWithText("Keep child in composition").performClick()
            onNodeWithText("Child left composition.", substring = true).assertExists()

            onNodeWithText("Derived level: Medium", substring = true).assertExists()
        }
}
