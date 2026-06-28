package org.jetbrains.compose.swing.test.screenshot

import org.jetbrains.compose.swing.test.SwingUiTest
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteraction
import org.jetbrains.compose.swing.test.interaction.SwingNodeInteractionCollection
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.CellRendererPane
import javax.swing.JPanel

/**
 * Renders the matched component, together with everything drawn inside it, to an off-screen image
 * exactly as it is currently laid out.
 *
 * The component must be displayed (attached under the test root with a non-zero size); this is the
 * same contract as [assertIsDisplayed]. Call after the composition has settled so the captured image
 * reflects the latest state.
 *
 * @return an image whose width and height match the component's laid-out size.
 * @throws AssertionError if the component is not displayed.
 */
public fun SwingNodeInteraction.captureToImage(): BufferedImage {
    assertIsDisplayed()
    return resolve().captureComponentToImage()
}

/**
 * Renders each matched component to its own off-screen image, in depth-first pre-order.
 *
 * Each component must be displayed; see [SwingNodeInteraction.captureToImage].
 *
 * @return one image per matched component, ordered to match the collection's other accessors.
 * @throws AssertionError if any matched component is not displayed.
 */
public fun SwingNodeInteractionCollection.captureToImages(): List<BufferedImage> =
    resolveAll().map { it.captureComponentToImage() }

/**
 * Renders the whole composition root, together with everything drawn inside it, to an off-screen
 * image. Equivalent to capturing the node returned by [SwingUiTest.onRoot].
 *
 * Call after the composition has settled so the captured image reflects the latest state.
 *
 * @return an image whose width and height match the root's laid-out size.
 * @throws AssertionError if the root has a zero laid-out size.
 */
public fun SwingUiTest.captureToImage(): BufferedImage = onRoot().captureToImage()

/**
 * Renders an arbitrary, hand-built raw AWT/Swing component to an off-screen image at exactly
 * [width] x [height], independently of any composition.
 *
 * This is the raw-Swing counterpart of [SwingNodeInteraction.captureToImage]: it lets a test render
 * a hand-written reference component (e.g. a plain `JButton`) through the same off-screen pipeline and
 * the same deterministic rendering hints used to capture composed components, so the two images can
 * be compared pixel-for-pixel.
 *
 * The component is given the requested bounds, laid out, and painted off screen; it is never shown on
 * screen. Size the capture to the laid-out bounds of the composed component you are comparing against
 * so both images share identical dimensions.
 *
 * The receiver must not already belong to another container.
 *
 * @param width the capture width in pixels; must be positive.
 * @param height the capture height in pixels; must be positive.
 * @return an image of exactly [width] x [height] pixels.
 * @throws IllegalArgumentException if [width] or [height] is not positive.
 */
public fun Component.captureToImage(
    width: Int,
    height: Int,
): BufferedImage {
    val component = this
    require(width > 0 && height > 0) {
        "Capture size must be positive but was ${width}x$height."
    }
    // Host the raw component in an off-screen container so it acquires a parent and a UI delegate,
    // exactly as a composed component does under the test root. A CellRendererPane is the standard
    // Swing vehicle for painting a component off-screen without realizing a window.
    val host =
        JPanel(null).apply {
            size = Dimension(width, height)
            preferredSize = Dimension(width, height)
        }
    val rendererPane = CellRendererPane()
    host.add(rendererPane)
    rendererPane.setBounds(0, 0, width, height)
    rendererPane.add(component)
    component.setBounds(0, 0, width, height)
    layoutContainerTree(host)

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        applyDeterministicRenderingHints(graphics)
        // See captureComponentToImage: printAll renders off-screen regardless of showing state.
        component.printAll(graphics)
    } finally {
        graphics.dispose()
    }
    return image
}

/** Lays out [container] and every descendant synchronously so each receives real bounds off-screen. */
private fun layoutContainerTree(container: Container) {
    container.doLayout()
    for (child in container.components) {
        if (child is Container) layoutContainerTree(child)
    }
}

/**
 * Captures [Component] into an image sized to its laid-out bounds, validating non-zero size.
 *
 * Paints the component and all its descendants with deterministic rendering hints so repeated
 * captures of an unchanged tree are stable.
 */
internal fun Component.captureComponentToImage(): BufferedImage {
    if (width <= 0 || height <= 0) {
        throw AssertionError(
            "Cannot capture ${javaClass.simpleName}: it has zero laid-out size (${width}x$height).",
        )
    }
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        applyDeterministicRenderingHints(graphics)
        // printAll (not paintAll) renders a component and its descendants to an arbitrary Graphics
        // regardless of on-screen showing state; paintAll early-returns for a component with no
        // realized peer, leaving the image blank. The harness runs headless with no realized window,
        // so the print path is what actually rasterizes the component's pixels off-screen.
        printAll(graphics)
    } finally {
        graphics.dispose()
    }
    return image
}

/**
 * Applies the rendering hints that make repeated captures of an unchanged tree byte-stable, so the
 * composed and raw capture paths rasterize identically.
 */
private fun applyDeterministicRenderingHints(graphics: java.awt.Graphics2D) {
    graphics.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
    )
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    graphics.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS,
        RenderingHints.VALUE_FRACTIONALMETRICS_OFF,
    )
    graphics.setRenderingHint(
        RenderingHints.KEY_STROKE_CONTROL,
        RenderingHints.VALUE_STROKE_PURE,
    )
}
