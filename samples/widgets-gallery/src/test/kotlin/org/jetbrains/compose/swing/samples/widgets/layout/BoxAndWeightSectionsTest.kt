package org.jetbrains.compose.swing.samples.widgets.layout

import org.jetbrains.compose.swing.samples.widgets.openSection
import org.jetbrains.compose.swing.test.ComposeSwingTest
import org.jetbrains.compose.swing.test.interaction.performClick
import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.runComposeSwingTest
import javax.swing.JLabel
import javax.swing.JSlider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Every card in these two sections is driven from a control and prints the placement or extent the
// layout granted. These tests drive the control and read that printout back, which is the same thing a
// person looking at the card does - so a card whose readout stops following its control fails here.
class BoxAndWeightSectionsTest {
    @Test
    fun theContentAlignmentRadioGroupMovesTheChildTheBoxPlaces() =
        runComposeSwingTest {
            openSection("Box")

            onNodeWithText("contentAlignment = Center,", substring = true).assertExists()
            val centered = readoutNumbers("contentAlignment = ")

            onNodeWithText("BottomEnd").performClick()
            awaitIdle()

            onNodeWithText("contentAlignment = BottomEnd", substring = true).assertExists()
            val bottomEnd = readoutNumbers("contentAlignment = ")
            assertTrue(
                bottomEnd[0] > centered[0] && bottomEnd[1] > centered[1],
                "BottomEnd places the front child further right and further down than Center did",
            )
        }

    @Test
    fun aChildThatNamesItsOwnAlignIgnoresTheBoxsContentAlignment() =
        runComposeSwingTest {
            openSection("Box")

            val cornerBefore = onNodeWithText("align(TopStart)").fetch<JLabel>().location
            onNodeWithText("Unaligned child sits at Center", substring = true).assertExists()

            onNodeWithText("Box places unaligned children at BottomCenter").performClick()
            awaitIdle()

            onNodeWithText("Unaligned child sits at BottomCenter", substring = true).assertExists()
            assertEquals(
                cornerBefore,
                onNodeWithText("align(TopStart)").fetch<JLabel>().location,
                "the corner child keeps its own placement while the box moves the unaligned one",
            )
        }

    @Test
    fun theBackdropTakesTheBoxsExtentOnlyWhileItDeclaresMatchParentSize() =
        runComposeSwingTest {
            openSection("Box")

            val matched = readoutNumbers("Backdrop: ")

            onNodeWithText("Backdrop declares matchParentSize").performClick()
            awaitIdle()

            val unmatched = readoutNumbers("Backdrop: ")
            assertTrue(
                unmatched[0] < matched[0] && unmatched[1] < matched[1],
                "without matchParentSize the backdrop falls back to the extent it asks for on its own",
            )
        }

    @Test
    fun theZIndexToggleBringsTheRaisedChildToTheFront() =
        runComposeSwingTest {
            openSection("Box")

            onNodeWithText("Front child: A").assertExists()
            // AWT paints the highest z-order index first, so the child in front is the one at index 0.
            assertTrue(zOrderOf("A") < zOrderOf("B"), "A starts in front of B")

            onNodeWithText("Raise B above A").performClick()
            awaitIdle()

            onNodeWithText("Front child: B").assertExists()
            assertTrue(zOrderOf("B") < zOrderOf("A"), "the raised child is painted over the one it ties with")
        }

    @Test
    fun aHeavierShareTakesWidthFromItsNeighborRatherThanFromTheRow() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            onNodeWithText("Second swatch weight: 3f").assertExists()
            val (lightBefore, heavyBefore, rowBefore) = readoutNumbers("Shares granted: ")

            sliders()[0].value = 5
            awaitIdle()

            onNodeWithText("Second swatch weight: 5f").assertExists()
            val (lightAfter, heavyAfter, rowAfter) = readoutNumbers("Shares granted: ")
            assertTrue(
                heavyAfter > heavyBefore,
                "the heavier swatch is granted more width; was $heavyBefore, now $heavyAfter",
            )
            assertTrue(
                lightAfter < lightBefore,
                "the swatch beside it gives up what the heavier one gains; was $lightBefore, now $lightAfter",
            )
            assertEquals(
                rowBefore,
                rowAfter,
                "the weights divide the width the row was offered, so the row itself never moves",
            )
        }

    @Test
    fun aSwatchTakesItsWholeShareOnlyOnceItFills() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            val (leftBefore, rightBefore) = readoutNumbers("Shares: left swatch ")
            assertTrue(leftBefore < rightBefore, "without fill the left swatch keeps only the width it prefers")

            onNodeWithText("Left swatch fills its share").performClick()
            awaitIdle()

            val (leftAfter, rightAfter) = readoutNumbers("Shares: left swatch ")
            assertEquals(rightAfter, leftAfter, "two equal shares are equal widths once both fill")
            assertEquals(rightBefore, rightAfter, "the other swatch's share never depended on its neighbor")
        }

    @Test
    fun aMaximumSizeCapsHowMuchOfItsShareAChildOccupies() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            onNodeWithText("Share taken: 80 px", substring = true).assertExists()

            onNodeWithText("Cap the swatch at 80 px").performClick()
            awaitIdle()

            val (taken, row) = readoutNumbers("Share taken: ")
            assertTrue(taken > 80, "the uncapped child takes the whole share the row grants it")
            assertTrue(taken <= row, "the share cannot exceed the row it comes out of")
        }

    @Test
    fun aRelativeArrangementMirrorsWhereAnAbsoluteOneHolds() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            val endBefore = readoutNumbers("End: leading child at x = ").first()
            val absoluteBefore = readoutNumbers("Absolute.Right: leading child at x = ").first()

            onNodeWithText("Right-to-left orientation (arrangement)").performClick()
            awaitIdle()

            assertNotEquals(
                endBefore,
                readoutNumbers("End: leading child at x = ").first(),
                "Arrangement.End follows the row's reading order",
            )
            assertEquals(
                absoluteBefore,
                readoutNumbers("Absolute.Right: leading child at x = ").first(),
                "Arrangement.Absolute.Right packs against the physical right edge under either orientation",
            )
        }

    @Test
    fun aRelativeAlignmentMirrorsWhereAnAbsoluteOneHolds() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            val startBefore = readoutNumbers("Start: child at x = ").first()
            val leftBefore = readoutNumbers("AbsoluteAlignment.Left: child at x = ").first()

            onNodeWithText("Right-to-left orientation (alignment)").performClick()
            awaitIdle()

            assertNotEquals(
                startBefore,
                readoutNumbers("Start: child at x = ").first(),
                "Alignment.Start follows the column's reading order",
            )
            assertEquals(
                leftBefore,
                readoutNumbers("AbsoluteAlignment.Left: child at x = ").first(),
                "AbsoluteAlignment.Left never mirrors",
            )
        }

    @Test
    fun theBiasSliderSlidesTheChildAcrossTheBox() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            val centered = readoutNumbers("Horizontal bias: ").last()

            sliders()[1].value = 100
            awaitIdle()

            onNodeWithText("Horizontal bias: 1.0", substring = true).assertExists()
            val slid = readoutNumbers("Horizontal bias: ").last()
            assertTrue(
                slid > centered,
                "a bias of 1.0 puts the child further right than a bias of 0.0; was $centered, now $slid",
            )
        }

    @Test
    fun droppingTheSharedBaselineMovesTheLabelOntoTheRowsOwnAlignment() =
        runComposeSwingTest {
            openSection("Weight & alignment")

            val onBaseline = readoutNumbers("Align by baseline: ").first()

            onNodeWithText("Align by baseline").performClick()
            awaitIdle()

            onNodeWithText("Align by baseline: off", substring = true).assertExists()
            assertNotEquals(
                onBaseline,
                readoutNumbers("Align by baseline: ").first(),
                "off, the label sits at the row's Bottom alignment instead of on the shared baseline",
            )
        }
}

private val integers = Regex("-?\\d+")

// The numbers a card printed, in the order they appear in its readout. Prefixes are unique per section,
// so the substring names exactly one label.
private suspend fun ComposeSwingTest.readoutNumbers(prefix: String): List<Int> {
    // A card measures itself from a componentListener, and a layout pass announces the geometry it
    // changed on the event queue rather than to the listener directly, so the announcement is dispatched
    // one gate after the pass that posts it. This gate is that later one: it delivers what the caller's
    // own settle left queued and then settles the write the listener makes, so the label being read
    // prints the layout it is standing in.
    awaitIdle()
    val readout = onNodeWithText(prefix, substring = true).fetch<JLabel>().text
    return integers.findAll(readout).map { it.value.toInt() }.toList()
}

private fun ComposeSwingTest.zOrderOf(text: String): Int {
    val child = onNodeWithText(text).fetch<JLabel>()
    return child.parent.getComponentZOrder(child)
}

// The section holds the weight slider first and the bias slider second, in the order the cards compose.
private fun ComposeSwingTest.sliders(): List<JSlider> {
    val sliders = onAllNodesOfType<JSlider>().fetchAll<JSlider>()
    assertEquals(2, sliders.size, "the section exposes a weight slider and a bias slider")
    return sliders
}
