package org.jetbrains.compose.swing.samples.widgets

import org.jetbrains.compose.swing.test.onAllNodesOfType
import org.jetbrains.compose.swing.test.runSwingUiTest
import org.jetbrains.compose.swing.test.screenshot.assertImageMatches
import org.jetbrains.compose.swing.test.screenshot.captureToImage
import javax.swing.JSlider
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavioral + screenshot coverage for the Canvas section. The surface reads its slider state at paint
 * time, so moving a slider repaints it. The tests assert the slider echo recomposes and capture the
 * hand-drawn surface to a real bitmap, confirming an unchanged surface re-captures identically.
 */
class CanvasSectionTest {
    @Test
    fun theSweepSliderEchoRecomposes() =
        runSwingUiTest {
            openSection("Canvas")

            onNodeWithText("Sweep: 70%", substring = true).assertExists()

            // The sweep slider is the second slider in the card (after the petal-count slider). Drive it
            // through its own JSlider; the echo label recomposes from the same hoisted state.
            val sliders = onAllNodesOfType<JSlider>().fetchAll<JSlider>()
            assertTrue(sliders.size >= 2, "the Canvas card exposes a petal slider and a sweep slider")
            sliders[1].value = 30
            awaitIdle()
            onNodeWithText("Sweep: 30%", substring = true).assertExists()
        }

    @Test
    fun theCanvasSurfaceRendersToAStableBitmapScreenshotTest() =
        runSwingUiTest {
            openSection("Canvas")

            // Capture the hand-drawn surface as a real bitmap; re-capturing the unchanged surface matches.
            val initial = onNodeWithTag(CANVAS_TAG).captureToImage()
            assertTrue(initial.width > 0 && initial.height > 0, "the captured surface has real size")

            // The surface is hand-painted edge to edge (a gradient backdrop under petals and a gauge), so
            // it must be substantially filled with many distinct colours. A blank/transparent surface —
            // e.g. an unwired snapshot observer that skips onDraw — would still "self-match" on
            // re-capture, so assert real paint coverage here, not just that the surface is stable.
            var painted = 0
            val colours = HashSet<Int>()
            for (y in 0 until initial.height) {
                for (x in 0 until initial.width) {
                    val argb = initial.getRGB(x, y)
                    if ((argb ushr 24) != 0) painted++
                    colours.add(argb)
                }
            }
            val total = initial.width * initial.height
            assertTrue(
                painted > total / 2,
                "the Canvas surface must be substantially painted, but only $painted/$total pixels were drawn",
            )
            assertTrue(
                colours.size > 50,
                "the hand-drawn surface must show many distinct colours, but only ${colours.size} were present",
            )

            onNodeWithTag(CANVAS_TAG).assertImageMatches(expected = initial)
        }
}
