package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.SwingMatcher
import org.jetbrains.compose.swing.test.runSwingUiTest
import javax.accessibility.AccessibleRole
import kotlin.test.Test

/**
 * Behavioral coverage for the Accessibility section. The accessibility SwingModifiers project metadata
 * onto the underlying component's AccessibleContext, so the tests assert that projection through the
 * harness's accessibility matchers, and exercise the keyboard affordances (mnemonic, defaultButton)
 * whose buttons drive a visible counter.
 */
class AccessibilitySectionTest {
    @Test
    fun theNamedFieldAdvertisesItsAccessibleMetadata() =
        runSwingUiTest {
            openSection("Accessibility")

            // The tagged field carries the name + description set through the accessibility modifiers,
            // which the AccessibleContext exposes to assistive technologies.
            onNode(
                SwingMatcher.hasTestTag(ACCESSIBLE_NAME_FIELD_TAG) and
                    SwingMatcher.hasAccessibleName("Search query"),
            ).assertExists()
            onNode(
                SwingMatcher.hasTestTag(ACCESSIBLE_NAME_FIELD_TAG) and
                    SwingMatcher.hasAccessibleDescription("Type a term to filter the results list."),
            ).assertExists()
        }

    @Test
    fun theCanvasReportsTheOverriddenAccessibleRole() =
        runSwingUiTest {
            openSection("Accessibility")

            // accessibleRole(SLIDER) overrides what the drawing surface reports; the matcher reads it back.
            onNode(
                SwingMatcher.hasTestTag(ACCESSIBLE_ROLE_CANVAS_TAG) and
                    SwingMatcher.hasAccessibleRole(AccessibleRole.SLIDER),
            ).assertExists()
        }

    @Test
    fun theMnemonicAndDefaultButtonsDriveTheirCounters() =
        runSwingUiTest {
            openSection("Accessibility")

            // The buttons are ordinary AbstractButtons; clicking each bumps its visible counter, proving
            // the keyboard-affordance modifiers do not break the plain click path.
            onNodeWithText("Saved 0 time(s)", substring = true).assertExists()
            onNodeWithText("Save").performClick()
            onNodeWithText("Saved 1 time(s)", substring = true).assertExists()

            onNodeWithText("Submitted 0 time(s)", substring = true).assertExists()
            onNodeWithText("Submit").performClick()
            onNodeWithText("Submitted 1 time(s)", substring = true).assertExists()
        }
}
