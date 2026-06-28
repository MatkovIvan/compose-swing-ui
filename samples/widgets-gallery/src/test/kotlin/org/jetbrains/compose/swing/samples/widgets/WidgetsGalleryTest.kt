package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.test.screenshot.assertImageMatches
import org.jetbrains.compose.swing.test.screenshot.captureToImage
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JProgressBar
import javax.swing.JSlider
import javax.swing.JViewport
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Behavioral coverage for the widgets-gallery showcase. The tests render the navigation shell, exercise
 * a representative slice of the leaf controls in the default Components section, then switch sections
 * through the sidebar list and assert the newly mounted section behaves. Each assertion drives a control
 * where a real gesture lands and reads the echo the wrapper's listener and recomposition produce — this
 * is a showcase, so the slice is representative rather than exhaustive.
 */
class WidgetsGalleryTest {
    @Test
    fun theDefaultSectionRendersTheButtonCounter() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            onNodeWithText("Counter: 0", substring = true).assertExists()
            onNodeWithText("Increment").performClick()
            onNodeWithText("Counter: 1", substring = true).assertExists()
            onNodeWithText("Reset").performClick()
            onNodeWithText("Counter: 0", substring = true).assertExists()
        }

    @Test
    fun theCheckBoxTogglesItsEcho() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            onNodeWithText("Feature is off", substring = true).assertExists()
            onNodeWithText("Enable feature").performClick()
            onNodeWithText("Feature is on", substring = true).assertExists()
        }

    @Test
    fun togglingTheCheckBoxDoesNotMoveIt() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            // The CheckBox shares a vertical BoxLayout column with a wider panel sibling (the radio row).
            // A column that mixes alignmentX values pushes the left-aligned (0.0) checkbox right to track
            // the centered (0.5) panel, so it no longer sits flush with the column's left edge and its
            // x-offset shifts whenever the column revalidates. Normalising every child keeps it at x = 0.
            val before = onNodeWithText("Enable feature").fetch<JCheckBox>().bounds
            assertTrue(before != Rectangle(), "the checkbox must have a real, laid-out bounds")
            assertEquals(0, before.x, "the checkbox must sit flush with the column's left edge")

            onNodeWithText("Enable feature").performClick()
            awaitIdle()
            val after = onNodeWithText("Enable feature").fetch<JCheckBox>().bounds
            assertEquals(before, after, "the checkbox must not move when toggled")
        }

    @Test
    fun theComboBoxSelectionFeedsTheEcho() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            onNodeWithText("Selected: Kotlin", substring = true).assertExists()

            val combo = onNodeWithTag(LANGUAGE_COMBO_TAG).fetch<JComboBox<*>>()
            combo.selectedIndex = 2
            awaitIdle()
            onNodeWithText("Selected: Scala", substring = true).assertExists()
        }

    @Test
    fun switchingToTheAnimationSectionMountsItsProgressBar() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            // Switch to the Animation section through the sidebar list, by its index in showcaseSections.
            val animationIndex = showcaseSections.indexOfFirst { it.title == "Animation" }
            assertTrue(animationIndex >= 0, "Animation section must be registered")

            val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
            list.selectedIndex = animationIndex
            awaitIdle()

            onNodeWithText("Section: Animation", substring = true).assertExists()

            // The eased ProgressBar starts at 0; clicking the 100% preset eventually drives it past 0.
            val bar = onNodeWithTag(ANIMATED_PROGRESS_TAG).fetch<JProgressBar>()
            assertEquals(0, bar.value)
            onNodeWithText("100%").performClick()
            waitUntil { bar.value > 0 }
            assertTrue(bar.value > 0, "the animated progress bar should advance toward the target")
        }

    @Test
    fun theAnimatedProgressBarStaysWithinItsScrollViewport() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
            list.selectedIndex = showcaseSections.indexOfFirst { it.title == "Animation" }
            awaitIdle()

            // A JProgressBar reports an unbounded maximum size, so in a vertical BoxLayout it would
            // otherwise stretch past the visible frame. Capping its maximum size keeps its right edge
            // inside the section's scroll viewport — no horizontal clipping or off-frame overflow.
            val bar = onNodeWithTag(ANIMATED_PROGRESS_TAG).fetch<JProgressBar>()
            val viewport = generateSequence(bar.parent) { it.parent }.filterIsInstance<JViewport>().first()
            val rightEdge = SwingUtilities.convertPoint(bar.parent, bar.x, bar.y, viewport).x + bar.width
            assertTrue(
                rightEdge <= viewport.width,
                "the progress bar (right edge $rightEdge) must fit within the viewport (${viewport.width})",
            )
        }

    @Test
    fun switchingToTheFormInputsSectionDrivesASpinnerEcho() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            val formIndex = showcaseSections.indexOfFirst { it.title == "Form inputs" }
            val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
            list.selectedIndex = formIndex
            awaitIdle()

            onNodeWithText("Section: Form inputs", substring = true).assertExists()
            onNodeWithText("Count is 3", substring = true).assertExists()

            // The ToggleButton card binds a pressed boolean to its echo; clicking it flips both.
            onNodeWithText("Bold is off", substring = true).assertExists()
            onNodeWithText("Bold").performClick()
            onNodeWithText("Bold is on", substring = true).assertExists()
        }

    @Test
    fun theCustomComponentSectionWrapsAJComponentThroughSwingNode() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            // Switch to the custom-component section through the sidebar list.
            val customIndex = showcaseSections.indexOfFirst { it.title == "Custom component" }
            assertTrue(customIndex >= 0, "Custom component section must be registered")
            val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
            list.selectedIndex = customIndex
            awaitIdle()

            onNodeWithText("Section: Custom component", substring = true).assertExists()

            // The custom StarRating widget reports clicks through its mouseListener, driving the sibling
            // "N / 5" echo composable — proof the hand-written JComponent is a first-class composition
            // citizen, not an island.
            onNodeWithText("3 / 5", substring = true).assertExists()
            clickStar(onNodeWithTag(STAR_RATING_TAG).fetch<JComponent>(), starIndex = 4)
            awaitIdle()
            onNodeWithText("5 / 5", substring = true).assertExists()
        }

    @Test
    fun theCustomWidgetRendersToAStableBitmapScreenshotTest() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            val customIndex = showcaseSections.indexOfFirst { it.title == "Custom component" }
            val list = onNodeWithTag(SECTION_LIST_TAG).fetch<JList<*>>()
            list.selectedIndex = customIndex
            awaitIdle()

            // Screenshot testing the custom widget: capture the hand-written JComponent as a real bitmap
            // and assert against it with `assertImageMatches`, which compares by structural similarity.
            val stars = onNodeWithTag(STAR_RATING_TAG).captureToImage()

            // The custom painting is stable: re-capturing the unchanged widget matches the first capture.
            onNodeWithTag(STAR_RATING_TAG).assertImageMatches(expected = stars)

            // A differently-sized element (the Clear button) does not match it; the comparison rejects it
            // on size alone, so the assertion throws.
            assertFailsWith<AssertionError> {
                onNodeWithTag(STAR_CLEAR_TAG).assertImageMatches(expected = stars)
            }
        }

    @Test
    fun theComponentsSliderDrivesItsProgressBar() =
        runSwingUiTest {
            setContent { ShowcaseShell() }

            onNodeWithText("Amount: 40", substring = true).assertExists()

            val slider = onNodeWithTag(AMOUNT_SLIDER_TAG).fetch<JSlider>()
            slider.value = 75
            awaitIdle()
            onNodeWithText("Amount: 75", substring = true).assertExists()
            assertEquals(75, slider.value)
        }

    /**
     * Dispatches a real mouse click onto the [starIndex]-th star (0-based) of the custom widget. The
     * stars are laid out as a row of square boxes, so the click lands at the box's horizontal centre.
     * This drives the widget's own MouseListener exactly as a user click would, which is why no public
     * `performClick` exists for it — it is not an AbstractButton.
     */
    private fun clickStar(
        component: JComponent,
        starIndex: Int,
    ) {
        val box = component.height
        val center = Point(starIndex * box + box / 2, box / 2)
        val event =
            MouseEvent(
                component,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                center.x,
                center.y,
                1,
                false,
            )
        component.dispatchEvent(event)
    }
}
