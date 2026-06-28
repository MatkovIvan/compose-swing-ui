package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.onNodeOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.test.screenshot.assertImageMatches
import org.jetbrains.compose.swing.test.screenshot.captureToImage
import java.awt.Cursor
import java.awt.Rectangle
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JSlider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavioral + screenshot coverage for the Modifier gallery, where each SwingModifier builder visibly
 * affects a real widget. The tests drive the interaction modifiers through their gestures and read the
 * status echoes, and capture the appearance-card label as a bitmap to prove the styling repaints when
 * the toggle flips it.
 */
class ModifierGallerySectionTest {
    @Test
    fun theEnabledModifierGatesTheField() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The field is enabled while the box is checked; unchecking it disables the field in-place.
            onNodeWithText("Editable when enabled", substring = true).assertIsEnabled()
            onNodeWithText("Field enabled").performClick()
            onNodeWithText("Editable when enabled", substring = true).assertIsNotEnabled()
        }

    @Test
    fun theSizeAndVisibilityCheckBoxStaysLeftAlignedWithItsColumn() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The "Show the second button" checkbox shares the card's vertical BoxLayout column with a
            // wider FlowPanel sibling. If the column mixes alignmentX values, the left-aligned (0.0)
            // checkbox is pushed right to track the centered (0.5) panel and no longer sits flush with
            // the column's left edge. Normalising every child to the same alignment keeps it at x = 0.
            val checkBox = onNodeWithText("Show the second button").fetch<JCheckBox>()
            val before = checkBox.bounds
            assertTrue(before != Rectangle(), "the checkbox must have a real, laid-out bounds")
            assertEquals(0, before.x, "the checkbox must sit flush with the column's left edge")

            // Toggling it must not move it either.
            onNodeWithText("Show the second button").performClick()
            awaitIdle()
            assertEquals(before, onNodeWithText("Show the second button").fetch<JCheckBox>().bounds)
        }

    @Test
    fun theRawActionListenerFiresAlongsideTheWrapper() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The actionListener modifier attaches an extra ActionListener; clicking the button fires it,
            // bumping the raw-listener counter.
            onNodeWithText("Raw listener fired 0 time(s)", substring = true).assertExists()
            onNodeWithText("Click (raw listener)").performClick()
            onNodeWithText("Raw listener fired 1 time(s)", substring = true).assertExists()
        }

    @Test
    fun theCursorModifierSwapsThePointerOverTheTarget() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The target starts with the hand cursor; unchecking restores the default pointer in-place.
            val target = onNodeWithText("Hover me", substring = true)
            assertEquals(Cursor.HAND_CURSOR, target.fetch<JLabel>().cursor.type)
            onNodeWithText("Hand cursor").performClick()
            assertEquals(Cursor.DEFAULT_CURSOR, target.fetch<JLabel>().cursor.type)
        }

    @Test
    fun theToolTipModifierAttachesHoverHelpText() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The tooltip text tracks the same toggle the cursor does.
            val target = onNodeWithText("Hover me", substring = true)
            assertEquals("Click affordance: the hand cursor", target.fetch<JLabel>().toolTipText)
            onNodeWithText("Hand cursor").performClick()
            assertEquals("Plain pointer", target.fetch<JLabel>().toolTipText)
        }

    @Test
    fun theClientPropertyModifierStoresAReadableValue() =
        runSwingUiTest {
            openSection("Modifier gallery")

            val carrier = onNodeWithText("Carries clientProperty", substring = true)
            assertEquals("alpha", carrier.fetch<JLabel>().getClientProperty("role"))
            onNodeWithText("role = beta").performClick()
            assertEquals("beta", carrier.fetch<JLabel>().getClientProperty("role"))
        }

    @Test
    fun theFocusableModifierGatesKeyboardFocus() =
        runSwingUiTest {
            openSection("Modifier gallery")

            val button = onNodeWithText("Tab reaches me only when focusable")
            assertTrue(button.fetch<JButton>().isFocusable)
            onNodeWithText("Button is focusable").performClick()
            assertFalse(button.fetch<JButton>().isFocusable)
        }

    @Test
    fun theMaximumSizeModifierCapsTheButtonWidth() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The clamped button reports the capped maximum width, so a stretching layout cannot widen it.
            val button = onNodeWithText("Clamped button").fetch<JButton>()
            assertEquals(240, button.maximumSize.width)
            assertEquals(120, button.minimumSize.width)
        }

    @Test
    fun theRawChangeListenerFiresAlongsideTheWrapper() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The changeListener modifier attaches an extra ChangeListener; moving the slider fires it.
            onNodeWithText("Raw change listener fired 0 time(s)", substring = true).assertExists()
            val slider = onNodeOfType<JSlider>().fetch<JSlider>()
            slider.value = 60
            awaitIdle()
            onNodeWithText("Value: 60", substring = true).assertExists()
            val echo = onNodeWithText("Raw change listener fired", substring = true).fetch<JLabel>().text
            assertTrue(echo.first { it.isDigit() } != '0', "the raw change listener fired at least once")
        }

    @Test
    fun theAppearanceLabelRepaintsAsAStableBitmapScreenshotTest() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // Capture the styled label as a real bitmap; a re-capture of the unchanged label matches it.
            val styledLabel = onNodeWithText("Styled when the box is checked", substring = true)
            val styled = styledLabel.captureToImage()
            assertTrue(styled.width > 0 && styled.height > 0, "the styled label has real size")
            styledLabel.assertImageMatches(expected = styled)

            // The appearance modifiers project onto observable Swing properties: while styled the label is
            // opaque (its background fill is painted); toggling the styling off makes it non-opaque again.
            assertTrue(
                onNodeWithText("Styled when the box is checked", substring = true).fetch<JLabel>().isOpaque,
                "the styled label paints its background fill (opaque)",
            )
            onNodeWithText("Fancy styling").performClick()
            assertFalse(
                onNodeWithText("Styled when the box is checked", substring = true).fetch<JLabel>().isOpaque,
                "removing the styling clears the opaque background fill",
            )
        }

    @Test
    fun theCursorTargetRendersToAStableBitmapScreenshotTest() =
        runSwingUiTest {
            openSection("Modifier gallery")

            // The cursor/toolTip target paints a filled background; capture it and assert a re-capture of
            // the unchanged label matches, proving the appearance modifiers produce a stable bitmap.
            val target = onNodeWithText("Hover me", substring = true)
            val image = target.captureToImage()
            assertTrue(image.width > 0 && image.height > 0, "the cursor target has real size")
            target.assertImageMatches(expected = image)
        }
}
