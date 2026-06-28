package org.jetbrains.compose.swing.test.screenshot

import org.jetbrains.compose.swing.components.Label
import org.jetbrains.compose.swing.components.button.Button
import org.jetbrains.compose.swing.test.runSwingUiTest
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreenshotTest {
    @Test
    fun captureProducesImageSizedToComponent() = runSwingUiTest {
        setContent { Button(text = "OK", onClick = {}) }

        val node = onNodeWithText("OK")
        val component = node.resolve()
        val image = node.captureToImage()

        assertTrue(image.width > 0 && image.height > 0, "captured image should have a positive size")
        assertEquals(component.width, image.width, "captured image width should match the component")
        assertEquals(component.height, image.height, "captured image height should match the component")
    }

    @Test
    fun rootCaptureMatchesRootSize() = runSwingUiTest {
        setContent { Label(text = "hi") }

        val image = captureToImage()
        assertEquals(root.width, image.width, "root capture width should match the root")
        assertEquals(root.height, image.height, "root capture height should match the root")
    }

    @Test
    fun captureMatchesItselfExactly() = runSwingUiTest {
        setContent { Button(text = "Click me", onClick = {}) }

        val image = onNodeWithText("Click me").captureToImage()
        val pixels = image.toArgbIntArray()
        val result = PixelPerfectMatcher.compare(pixels, pixels.copyOf(), image.width, image.height)

        assertTrue(result.matches, result.statistics)
    }

    @Test
    fun captureMatchesItselfStructurally() = runSwingUiTest {
        setContent { Button(text = "Click me", onClick = {}) }

        val image = onNodeWithText("Click me").captureToImage()
        // MSSIM of an image against itself is exactly 1.0, comfortably above the default threshold.
        assertImageMatches(expected = image, image = image)
    }

    @Test
    fun smallElementMoveStillPassesDefaultThreshold() = runSwingUiTest {
        val expected = renderWithElementAt(elementX = ELEMENT_X)
        val nudged = renderWithElementAt(elementX = ELEMENT_X + NUDGE_PIXELS)

        // Nudging a single element a couple of pixels keeps MSSIM above the default threshold.
        assertImageMatches(expected = expected, image = nudged)
    }

    @Test
    fun clearlyDifferentImageFails() = runSwingUiTest {
        setContent { Button(text = "Click me", onClick = {}) }

        val image = onNodeWithText("Click me").captureToImage()
        val inverted = invert(image)

        assertFailsWith<AssertionError> {
            assertImageMatches(expected = image, image = inverted)
        }
    }

    @Test
    fun sizeMismatchFails() = runSwingUiTest {
        setContent { Label(text = "hi") }

        val image = onNodeWithText("hi").captureToImage()
        val smaller = BufferedImage(image.width + 10, image.height, BufferedImage.TYPE_INT_ARGB)

        assertFailsWith<AssertionError> {
            assertImageMatches(expected = smaller, image = image)
        }
    }

    @Test
    fun pixelPerfectMatcherFlagsAnElementMove() {
        val expected = renderWithElementAt(elementX = ELEMENT_X)
        val nudged = renderWithElementAt(elementX = ELEMENT_X + NUDGE_PIXELS)

        val result =
            PixelPerfectMatcher.compare(
                expected.toArgbIntArray(),
                nudged.toArgbIntArray(),
                expected.width,
                expected.height,
            )

        assertTrue(!result.matches, "expected a pixel-perfect mismatch")
        assertNotNull(result.diff, "a diff image should be produced on mismatch")
    }

    @Test
    fun invalidGoldenIdentifierIsRejected() = runSwingUiTest {
        setContent { Label(text = "hi") }

        val image = onNodeWithText("hi").captureToImage()
        assertFailsWith<IllegalArgumentException> {
            assertImageAgainstGolden(image, "bad name!")
        }
    }

    @Test
    fun missingGoldenWithoutUpdateFails() = runSwingUiTest {
        setContent { Label(text = "hi") }

        val image = onNodeWithText("hi").captureToImage()
        assertFailsWith<AssertionError> {
            assertImageAgainstGolden(image, "definitely-not-recorded-golden")
        }
    }

    /**
     * Renders a small button-like image with a single low-contrast element positioned at
     * [elementX]. Rendering the same scene twice with element positions a couple of pixels apart
     * models "one UI element moved slightly": the change is structurally small (MSSIM stays above
     * the default threshold) yet not pixel-identical (the strict matcher still flags it).
     */
    private fun renderWithElementAt(elementX: Int): BufferedImage {
        val image = BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = Color(0xEE, 0xEE, 0xEE)
        graphics.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT)
        graphics.color = Color(0xD2, 0xD2, 0xD2)
        graphics.fillRect(elementX, ELEMENT_Y, ELEMENT_WIDTH, ELEMENT_HEIGHT)
        graphics.dispose()
        return image
    }

    private fun invert(source: BufferedImage): BufferedImage {
        val copy = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val argb = source.getRGB(x, y)
                val alpha = argb and -0x1000000
                val inverted = alpha or (argb.inv() and 0x00FFFFFF)
                copy.setRGB(x, y, inverted)
            }
        }
        return copy
    }

    private companion object {
        const val SCENE_WIDTH = 88
        const val SCENE_HEIGHT = 25
        const val ELEMENT_X = 36
        const val ELEMENT_Y = 10
        const val ELEMENT_WIDTH = 16
        const val ELEMENT_HEIGHT = 5
        const val NUDGE_PIXELS = 2
    }
}
